//"Jenkins Pipeline is a suite of plugins which supports implementing and integrating continuous delivery pipelines into Jenkins. Pipeline provides an extensible set of tools for modeling delivery pipelines "as code" via the Pipeline DSL."
//More information can be found on the Jenkins Documentation page https://jenkins.io/doc/
pipeline {
    agent { label 'linux-large' }
    options {
        buildDiscarder(logRotator(numToKeepStr:'25'))
        disableConcurrentBuilds()
        timestamps()
    }
    triggers {
        /*
          Restrict nightly builds to master branch, all others will be built on change only.
          Note: The BRANCH_NAME will only work with a multi-branch job using the github-branch-source
        */
        cron(BRANCH_NAME == "master" ? "H H(17-19) * * *" : "")
    }
    environment {
        ITESTS = 'tests/itests'
        LARGE_MVN_OPTS = '-Xmx8192M -Xss128M -XX:+CMSClassUnloadingEnabled -XX:+UseConcMarkSweepGC '
        LINUX_MVN_RANDOM = '-Djava.security.egd=file:/dev/./urandom'
    }
    stages {
        stage('Setup') {
            steps {
                slackSend color: 'good', message: "STARTED: ${JOB_NAME} ${BUILD_NUMBER} ${BUILD_URL}"
            }
        }
        stage('Build/Test') {
            parallel {
                stage('Linux Full Build') {
                    when { expression { env.CHANGE_ID == null } }
                    steps {
                        withMaven(maven: 'Maven 3.5.2', globalMavenSettingsConfig: 'default-global-settings', mavenSettingsConfig: 'codice-maven-settings', mavenOpts: '${LARGE_MVN_OPTS} ${LINUX_MVN_RANDOM}') {
                            sh 'mvn install -B -nsu'
                        }
                    }
                }
                stage('Windows Full Build') {
                    when { expression { env.CHANGE_ID == null } }
                    agent { label 'server-2016-large' }
                    steps {
                        withMaven(maven: 'Maven 3.5.2', jdk: 'jdk8-latest', globalMavenSettingsConfig: 'default-global-settings', mavenSettingsConfig: 'codice-maven-settings', mavenOpts: '${LARGE_MVN_OPTS}') {
                            bat 'mvn install -B -nsu'
                        }
                    }
                }
                stage('Linux PR Build') {
                    when {
                        allOf {
                            expression { env.CHANGE_ID != null }
                            expression { env.CHANGE_TARGET != null }
                        }
                    }
                    steps {
                        withMaven(maven: 'Maven 3.5.2', globalMavenSettingsConfig: 'default-global-settings', mavenSettingsConfig: 'codice-maven-settings', mavenOpts: '${LARGE_MVN_OPTS} ${LINUX_MVN_RANDOM}', options: [artifactsPublisher(disabled: true), dependenciesFingerprintPublisher(disabled: true, includeScopeCompile: false, includeScopeProvided: false, includeScopeRuntime: false, includeSnapshotVersions: false)]) {
                            sh 'mvn install -B -Dfindbugs.skip=true -Dpmd.skip=true -Dcheckstyle.skip=true -DskipTests=true -nsu'
                            sh 'mvn install -B -Dgib.enabled=true -Dgib.referenceBranch=/refs/remotes/origin/$CHANGE_TARGET -nsu'
                        }
                    }
                }
                stage('Windows PR Build') {
                    when {
                        allOf {
                            expression { env.CHANGE_ID != null }
                            expression { env.CHANGE_TARGET != null }
                        }
                    }
                    agent { label 'server-2016-large' }
                    steps {
                        withMaven(maven: 'Maven 3.5.2', jdk: 'jdk8-latest', globalMavenSettingsConfig: 'default-global-settings', mavenSettingsConfig: 'codice-maven-settings', mavenOpts: '${LARGE_MVN_OPTS}', options: [artifactsPublisher(disabled: true), dependenciesFingerprintPublisher(disabled: true, includeScopeCompile: false, includeScopeProvided: false, includeScopeRuntime: false, includeSnapshotVersions: false)]) {
                            bat 'mvn install -B -Dfindbugs.skip=true -Dpmd.skip=true -Dcheckstyle.skip=true -DskipTests=true -nsu'
                            bat 'mvn install -B -Dgib.enabled=true -Dgib.referenceBranch=/refs/remotes/origin/%CHANGE_TARGET% -nsu'
                        }
                    }
                }
            }
        }
        stage('Security Analysis') {
            // Add additional things like owasp later
            parallel {
                stage('OWASP') {
                    steps {
                        echo "OWASP Scans coming soon!"
                    }
                }
                stage('nodeJsSecurity') {
                    agent { label 'linux-small' }
                    steps {
                        script {
                            def packageFiles = findFiles(glob: '**/package.json')
                            for (int i = 0; i < packageFiles.size(); i++) {
                                dir(packageFiles[i].path.split('package.json')[0]) {
                                    def packageFile = readJSON file: 'package.json'
                                    if (packageFile.scripts =~ /.*webpack.*/ || packageFile.containsKey("browserify")) {
                                        nodejs(configId: 'npmrc-default', nodeJSInstallationName: 'nodejs') {
                                            echo "Scanning ${packageFiles[i].path}"
                                            sh 'nsp check'
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        /*
          Deploy stage will only be executed for deployable branches. These include master and any patch branch matching M.m.x format (i.e. 2.10.x, 2.9.x, etc...).
          It will also only deploy in the presence of an environment variable JENKINS_ENV = 'prod'. This can be passed in globally from the jenkins master node settings.
        */
        stage('Deploy') {
            when {
                allOf {
                    expression { env.CHANGE_ID == null }
                    expression { env.BRANCH_NAME ==~ /((?:\d*\.)?\d*\.x|master)/ }
                    environment name: 'JENKINS_ENV', value: 'prod'
                }
            }
            steps{
                withMaven(maven: 'Maven 3.5.2', jdk: 'jdk8-latest', globalMavenSettingsConfig: 'default-global-settings', mavenSettingsConfig: 'codice-maven-settings', mavenOpts: '${LINUX_MVN_RANDOM}') {
                    sh 'mvn javadoc:aggregate -B -Dfindbugs.skip=true -Dpmd.skip=true -Dcheckstyle.skip=true -DskipTests=true -nsu'
                    sh 'mvn deploy -B -Dfindbugs.skip=true -Dpmd.skip=true -Dcheckstyle.skip=true -DskipTests=true -DretryFailedDeploymentCount=10 -nsu'
                }
            }
        }
    }
    post {
        success {
            slackSend color: 'good', message: "SUCCESS: ${JOB_NAME} ${BUILD_NUMBER}"
        }
        failure {
            slackSend color: '#ea0017', message: "FAILURE: ${JOB_NAME} ${BUILD_NUMBER}. See the results here: ${BUILD_URL}"
        }
        unstable {
            slackSend color: '#ffb600', message: "UNSTABLE: ${JOB_NAME} ${BUILD_NUMBER}. See the results here: ${BUILD_URL}"
        }
    }
}
