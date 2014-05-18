$(document).ready () ->
  id = '#route_table'
  vue = new Vue
    el: id
    data:
      routes: []
      cellInfo: []
      counts: []
      area: 0
      info: 0
    methods:
      getJSON: () ->
        $.getJSON "/rest/v1/route/#{@area}/#{@info}", (data) =>
          @routes = data
          sumCounts = []
          data.forEach (d) ->
            sumCounts[d.dep] ?= 0
            sumCounts[d.dep] += d.count
          @counts = sumCounts
        $.getJSON "/rest/v1/cell_info", {area: @area, info: @info}, (data) =>
          @cellInfo = data
      viewCell: (cell) ->
        cInfo = (@cellInfo.filter (c) -> c.cell == cell)[0]
        "#{cell}" +
          if cInfo?
            "(#{cInfo.alphabet})" +
            if cInfo.start then ' <small>Start</small>' else '' +
            if cInfo.boss then ' <small>BOSS</small>' else ''
          else
            ''
      viewRate: (route) ->
        v = route.count / @counts[route.dep] * 100
        v.toFixed(1) + '%'
      loadAttr: (el) ->
        @area = parseInt($(el).attr('data-area'))
        @info = parseInt($(el).attr('data-info'))
    created: () ->
      @loadAttr(id)
      @getJSON()
