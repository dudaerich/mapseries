
class Events
  constructor: () ->
    @events = {}

  on: (event, callback) ->
    this._getEvent(event).push(callback)

  fire: (event, thisArg, args...) ->
    callback.apply(thisArg, args) for callback in this._getEvent(event)

  _getEvent: (event) ->
    if @events[event]
      @events[event]
    else
      @events[event] = []
      @events[event]

events = new Events()

export default events
