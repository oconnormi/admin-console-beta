/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.admin.ldap.commons;

import static org.forgerock.opendj.ldap.schema.ObjectClassType.AUXILIARY;
import static org.forgerock.opendj.ldap.schema.ObjectClassType.STRUCTURAL;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.codice.ddf.admin.ldap.fields.query.LdapTypeField;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.DN;
import org.forgerock.opendj.ldap.Entries;
import org.forgerock.opendj.ldap.Filter;
import org.forgerock.opendj.ldap.LdapException;
import org.forgerock.opendj.ldap.RootDSE;
import org.forgerock.opendj.ldap.SearchResultReferenceIOException;
import org.forgerock.opendj.ldap.SearchScope;
import org.forgerock.opendj.ldap.requests.Requests;
import org.forgerock.opendj.ldap.requests.SearchRequest;
import org.forgerock.opendj.ldap.responses.SearchResultEntry;
import org.forgerock.opendj.ldap.schema.AttributeType;
import org.forgerock.opendj.ldap.schema.ObjectClass;
import org.forgerock.opendj.ldap.schema.Schema;
import org.forgerock.opendj.ldif.ConnectionEntryReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <b> This code is experimental. While this class is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 */
public abstract class ServerGuesser {
  public static final Logger LOGGER = LoggerFactory.getLogger(ServerGuesser.class);
  private static final String ROOT_DOMAIN_NAMING_CONTEXT = "rootDomainNamingContext";
  private static final String NAMING_CONTEXTS = "namingContexts";
  private static final String MEMBER = "member";

  @SuppressWarnings({
    "StaticInitializerReferencesSubClass",
    "squid:S2390" /* Accessing sub-methods */
  })
  private static final Map<String, Function<Connection, ServerGuesser>> GUESSER_LOOKUP =
      ImmutableMap.of(
          LdapTypeField.ActiveDirectory.ACTIVE_DIRECTORY,
          ServerGuesser.ADGuesser::new,
          LdapTypeField.EmbeddedLdap.EMBEDDED,
          ServerGuesser.EmbeddedGuesser::new,
          LdapTypeField.OpenLdap.OPEN_LDAP,
          ServerGuesser.OpenLdapGuesser::new,
          LdapTypeField.OpenDJ.OPEN_DJ,
          ServerGuesser.OpenDjGuesser::new,
          LdapTypeField.UnknownEnumValue.UNKNOWN,
          DefaultGuesser::new);

  protected final Connection connection;

  private static final Predicate<ObjectClass> STRUCT_OR_AUX =
      oc -> oc.getObjectClassType() == STRUCTURAL || oc.getObjectClassType() == AUXILIARY;

  private ServerGuesser(Connection connection) {
    this.connection = connection;
  }

  public static ServerGuesser buildGuesser(String ldapType, Connection connection) {
    return Optional.ofNullable(GUESSER_LOOKUP.get(ldapType))
        .orElse(DefaultGuesser::new)
        .apply(connection);
  }

  public static ServerGuesser buildGuesser(Connection connection) {
    return buildGuesser(null, connection);
  }

  public List<String> getBaseContexts() {
    try {
      ConnectionEntryReader reader =
          connection.search("", SearchScope.BASE_OBJECT, "(objectClass=*)", NAMING_CONTEXTS);

      ArrayList<String> contexts = new ArrayList<>();
      while (reader.hasNext()) {
        SearchResultEntry entry = reader.readEntry();
        if (entry.containsAttribute(NAMING_CONTEXTS)) {
          contexts.add(entry.getAttribute(NAMING_CONTEXTS).firstValueAsString());
        }
      }

      if (contexts.isEmpty()) {
        contexts.add("");
      }
      return contexts;
    } catch (LdapException | SearchResultReferenceIOException e) {
      LOGGER.debug("Error getting baseContext", e);
      return Collections.singletonList("");
    }
  }

  public List<String> getLoginUserAttribute() {
    return ImmutableList.of("uid");
  }

  public List<String> getGroupObjectClass() {
    return ImmutableList.of("groupOfNames", "group", "posixGroup");
  }

  public List<String> getGroupAttributeHoldingMember() {
    return ImmutableList.of(MEMBER, "uniqueMember", "memberUid");
  }

  public List<String> getMemberAttributeReferencedInGroup() {
    return ImmutableList.of("uid");
  }

  public List<String> getUserBaseChoices() {
    return getChoices("(|(ou=user*)(name=user*)(cn=user*))");
  }

  public List<String> getGroupBaseChoices() {
    return getChoices("(|(ou=group*)(name=group*)(cn=group*)(objectClass=groupOfUniqueNames))");
  }

  public Set<String> getClaimAttributeOptions(String baseUserDn) {
    try {
      // Find all object classes with names like *person* in the core schema
      // this will catch person, organizationalPerson, inetOrgPerson, etc. if present
      SortedSet<String> attributes =
          extractAttributes(
              Schema.getCoreSchema().getObjectClasses(),
              oc -> oc.getNameOrOID().toLowerCase().matches(".*person.*"));

      // Find any given user with the clearance attribute
      SearchRequest clearanceReq =
          Requests.newSearchRequest(
              DN.valueOf(baseUserDn),
              SearchScope.WHOLE_SUBTREE,
              Filter.present("2.16.840.1.101.2.2.1.203"),
              "objectClass");
      ConnectionEntryReader clearanceReader = connection.search(clearanceReq);

      if (clearanceReader.hasNext()) {
        SearchResultEntry entry = clearanceReader.readEntry();
        RootDSE rootDSE = RootDSE.readRootDSE(connection);
        DN subschemaDN = rootDSE.getSubschemaSubentry();
        Schema subschema = Schema.readSchema(connection, subschemaDN);

        // Check against both the subschema and the default schema
        attributes.addAll(
            extractAttributes(Entries.getObjectClasses(entry, subschema), STRUCT_OR_AUX));
        attributes.addAll(extractAttributes(Entries.getObjectClasses(entry), STRUCT_OR_AUX));
      }
      return attributes;
    } catch (SearchResultReferenceIOException | LdapException e) {
      LOGGER.warn(
          "Error retrieving attributes from LDAP server; this may indicate a configuration issue with config.");
      return Collections.emptySet();
    }
  }

  private SortedSet<String> extractAttributes(
      Collection<ObjectClass> objectClasses, Predicate<ObjectClass> predicate) {
    return objectClasses
        .stream()
        .filter(predicate)
        .flatMap(oc -> Sets.union(oc.getRequiredAttributes(), oc.getOptionalAttributes()).stream())
        .map(AttributeType::getNameOrOID)
        .collect(Collectors.toCollection(TreeSet::new));
  }

  private List<String> getChoices(String query) {
    List<String> baseContexts = getBaseContexts();

    List<String> choices = new ArrayList<>();
    for (String baseContext : baseContexts) {
      try (ConnectionEntryReader reader =
          connection.search(baseContext, SearchScope.WHOLE_SUBTREE, query)) {
        while (reader.hasNext()) {
          if (!reader.isReference()) {
            SearchResultEntry resultEntry = reader.readEntry();
            choices.add(resultEntry.getName().toString());
          } else {
            // TODO RAP 07 Dec 16: What do we need to do with remote references?
            reader.readReference();
          }
        }
      } catch (IOException e) {
        LOGGER.debug("Error getting choices", e);
      }
    }

    return choices;
  }

  private static class DefaultGuesser extends ServerGuesser {
    private DefaultGuesser(Connection connection) {
      super(connection);
    }
  }

  private static class ADGuesser extends ServerGuesser {
    private static final Predicate<String> USER_DN_EXC =
        Pattern.compile(
                ".*(,|^)cn=system(,|$).*|.*(,|^)cn=builtin(,|$).*", Pattern.CASE_INSENSITIVE)
            .asPredicate()
            .negate();

    private static final Predicate<String> GROUP_DN_EXC =
        Pattern.compile(".*(,|^)cn=group policy creator owners(,|$).*", Pattern.CASE_INSENSITIVE)
            .asPredicate()
            .negate();

    @SuppressWarnings("squid:UnusedPrivateMethod" /* Constructor used in the GUESSER_LOOKUP map */)
    private ADGuesser(Connection connection) {
      super(connection);
    }

    @Override
    public List<String> getBaseContexts() {
      try {
        ConnectionEntryReader reader =
            connection.search(
                "", SearchScope.BASE_OBJECT, "(objectClass=*)", ROOT_DOMAIN_NAMING_CONTEXT);

        if (reader.hasNext()) {
          SearchResultEntry entry = reader.readEntry();
          if (entry.containsAttribute(ROOT_DOMAIN_NAMING_CONTEXT)) {
            return Collections.singletonList(
                entry.getAttribute(ROOT_DOMAIN_NAMING_CONTEXT).firstValueAsString());
          } else {
            return Collections.singletonList("");
          }
        } else {
          return Collections.singletonList("");
        }
      } catch (LdapException | SearchResultReferenceIOException e) {
        LOGGER.debug("Error getting baseContext", e);
        return Collections.singletonList("");
      }
    }

    @Override
    public List<String> getLoginUserAttribute() {
      return Collections.singletonList("sAMAccountName");
    }

    @Override
    public List<String> getGroupObjectClass() {
      return Collections.singletonList("group");
    }

    @Override
    public List<String> getGroupAttributeHoldingMember() {
      return Collections.singletonList(MEMBER);
    }

    @Override
    public List<String> getUserBaseChoices() {
      return super.getUserBaseChoices().stream().filter(USER_DN_EXC).collect(Collectors.toList());
    }

    @Override
    public List<String> getGroupBaseChoices() {
      return super.getGroupBaseChoices().stream().filter(GROUP_DN_EXC).collect(Collectors.toList());
    }
  }

  private static class EmbeddedGuesser extends ServerGuesser {
    @SuppressWarnings("squid:UnusedPrivateMethod" /* Constructor used in the GUESSER_LOOKUP map */)
    private EmbeddedGuesser(Connection connection) {
      super(connection);
    }

    @Override
    public List<String> getUserBaseChoices() {
      return Collections.singletonList("ou=users,dc=example,dc=com");
    }

    @Override
    public List<String> getGroupBaseChoices() {
      return Collections.singletonList("ou=groups,dc=example,dc=com");
    }

    @Override
    public List<String> getGroupObjectClass() {
      return Collections.singletonList("groupOfNames");
    }

    @Override
    public List<String> getGroupAttributeHoldingMember() {
      return Collections.singletonList(MEMBER);
    }
  }

  private static class OpenLdapGuesser extends ServerGuesser {
    @SuppressWarnings("squid:UnusedPrivateMethod" /* Constructor used in the GUESSER_LOOKUP map */)
    private OpenLdapGuesser(Connection connection) {
      super(connection);
    }
  }

  private static class OpenDjGuesser extends ServerGuesser {
    @SuppressWarnings("squid:UnusedPrivateMethod" /* Constructor used in the GUESSER_LOOKUP map */)
    private OpenDjGuesser(Connection connection) {
      super(connection);
    }
  }
}
