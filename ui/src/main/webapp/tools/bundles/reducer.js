import { combineReducers } from 'redux-immutable'

import sub from 'redux-submarine'

export const submarine = sub()
export const getSelected = (state) => submarine(state).get('selected')

export const select = (selected) => ({ type: 'bundles/SELECT', selected })

const selected = (state = [], { type, selected }) => {
  switch (type) {
    case 'bundles/SELECT':
      return selected
    default:
      return state
  }
}

export default combineReducers({ selected })
