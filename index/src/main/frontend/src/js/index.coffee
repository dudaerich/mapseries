
updateLayout = () ->
  rightPanelHeaderHeight = 0
  $('.right-panel .red').each (i, v) -> rightPanelHeaderHeight += $(v).outerHeight()
  $('.top-panel').css('height', "#{rightPanelHeaderHeight}px")
  $('.main').css('top', "#{rightPanelHeaderHeight}px")

export default {
  main: ->
    $(() ->
      updateLayout()
    )
    $(window).on('resize', updateLayout)
}
