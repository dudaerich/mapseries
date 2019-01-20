import $ from 'jquery'

updateScrollbar = ->
  func = ->
    container = $('.main.sheet .left-panel')
    activeItem = $('.main.sheet .left-panel .item.active')
    activeItemTop = Math.round(activeItem.position().top)
    activeItemBottom = activeItemTop + activeItem.height()

    if activeItemBottom > $(window).height() || activeItemTop < 0
      container.scrollTop(activeItemTop - 20)

  window.setTimeout(func, 500)

registerKeyDownListener = ->
  LEFT_KEY = 37
  RIGHT_KEY = 39

  movePage = (direction) ->
    a = $(".main.sheet .detail .arrow.#{direction}")
    if a.length
      window.location = a.attr('href')

  $(document).keydown (e) ->
    if e.keyCode == LEFT_KEY
      e.preventDefault()
      movePage('left')
    else if e.keyCode == RIGHT_KEY
      e.preventDefault()
      movePage('right')

updateLayout = ->
  metadata = $('.main.sheet .detail .metadata-container')
  metadataTop = metadata.offset().top
  windowHeight = $(window).height()
  metadataHeight = Math.round(windowHeight - metadataTop)
  metadata.css('height', "#{metadataHeight}px")

export default {
  main: ->
    $(() ->
      updateScrollbar()
      registerKeyDownListener()
      updateLayout()
    )
    $(window).on('resize', updateLayout)
}
