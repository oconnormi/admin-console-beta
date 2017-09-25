import React from 'react'
import { connect } from 'react-redux'

import { gql, graphql } from 'react-apollo'

import Flexbox from 'flexbox-react'

import { getSelected, select } from './reducer'

const Graph = (props) => {
  const Vis = require('react-graph-vis').default
  return <Vis {...props} />
}

const bundleGraph = (bundles, selected = []) => {
  const edges = bundles
    .map(({ id, refLists = [], refs = [] }) => {
      const refListsEdges = refLists
        .map(({ resolution, services }) =>
          services.map((service) => ({
            from: id,
            to: service.bundleId,
            color: 'red',
            title: service.name,
            dashes: resolution === 'optional'
          })))
        .reduce((a, b) => a.concat(b), [])

      const refEdges = refs.map(({ resolution, service }) => ({
        from: id,
        to: service.bundleId,
        color: 'blue',
        title: service.name,
        dashes: resolution === 'optional'
      }))

      return refListsEdges.concat(refEdges)
    })
    .reduce((a, b) => a.concat(b), [])
    .filter(({ from, to }) =>
      selected.length > 0
       ? selected.includes(from) || selected.includes(to)
       : true)

  const nodes = bundles.map(({ id, name }) => {
    const _selected = selected.includes(id)
    const shown = selected.length > 0
        ? _selected || edges.some(({ from, to }) => id === from || id === to)
        : true

    return {
      id,
      title: name,
      label: id,
      physics: !_selected,
      hidden: !shown
    }
  })

  return { nodes, edges }
}

const options = {
  autoResize: true,
  interaction: {
    hover: true,
    tooltipDelay: 0,
    navigationButtons: true,
    multiselect: true
  },
  nodes: {
    shape: 'box',
    color: {
      border: 'rgb(24, 188, 156)',
      background: 'rgb(24, 188, 156)'
    }
  },
  edges: {
    color: '#000000'
  }
}

const styles = `
  .vis-tooltip {
    position: absolute;
    font-size: 1.1em;
  }
`

const Inspector = (props) => {
  const {
    data: {
      dev = { bundles: [] }
    },
    selected = [],
    onSelect
  } = props

  const events = {
    select: ({ nodes }) => onSelect(nodes)
  }

  const bundles = dev.bundles
    .filter(({ location }) => location.match('ddf'))

  const graph = bundleGraph(bundles, selected)

  return (
    <div style={{ position: 'fixed', top: 64, bottom: 0, left: 0, right: 0 }}>
      <h1>Bundles</h1>
      <style>{styles}</style>
      <Flexbox>
        <Flexbox width='320px'>
          <div style={{ height: 960, width: '100%', background: 'white', padding: 10, overflowY: 'scroll' }}>
            {bundles
              .filter(({ id }) => selected.includes(id))
              .map((bundle, i) => {
                return (
                  <div key={bundle.id}>
                    <span>{bundle.id}: {bundle.name}</span>
                    <pre>
                      {/* JSON.stringify(bundle, null, 2) */}
                    </pre>
                  </div>
                )
              })}
          </div>
        </Flexbox>
        <Flexbox flex='1' width='100%'>
          <Graph graph={graph} options={options} events={events} />
        </Flexbox>
      </Flexbox>
    </div>
  )
}

const Connected = connect(
  (state) => ({
    selected: getSelected(state)
  }),
  { onSelect: select }
)(Inspector)

const Bundles = gql`
  query Bundle {
    dev {
      bundles {
        id
        name
        location
        refs {
          interface
          resolution
          service {
            name
            bundleId
          }
        }
        refLists {
          interface
          resolution
          services {
            name
            bundleId
          }
        }
      }
    }
  }
`

export default graphql(Bundles)(Connected)

