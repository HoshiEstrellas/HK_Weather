const { request, formatDateTime, fixed } = require('../../utils/request')

Page({
  data: {
    keyword: '',
    history: [],
    loading: true,
    error: ''
  },

  onLoad() {
    this.loadData()
  },

  onPullDownRefresh() {
    this.loadData().finally(() => wx.stopPullDownRefresh())
  },

  onKeywordInput(event) {
    this.setData({
      keyword: event.detail.value
    })
  },

  onSearch() {
    this.loadData()
  },

  loadData() {
    this.setData({ loading: true, error: '' })
    return request({ url: '/history', data: { q: this.data.keyword, limit: 300 } })
      .then((history) => {
        this.setData({
          history: history.map((item) => this.decorateObservation(item))
        })
      })
      .catch((err) => {
        this.setData({ error: err.message || '加载失败' })
      })
      .finally(() => {
        this.setData({ loading: false })
      })
  },

  openDetail(event) {
    const { lp, device } = event.currentTarget.dataset
    wx.navigateTo({
      url: `/pages/detail/detail?lpNumber=${encodeURIComponent(lp)}&deviceId=${encodeURIComponent(device)}`
    })
  },

  decorateObservation(item) {
    return {
      ...item,
      temperatureText: `${fixed(item.temperatureC, 1)}°C`,
      humidityText: `${fixed(item.humidityPercent, 0)}%`,
      windSpeedText: fixed(item.windSpeed, 1),
      windDirectionText: item.windDirectionDeg === null || item.windDirectionDeg === undefined ? '--' : `${item.windDirectionDeg}°`,
      observedText: formatDateTime(item.sourceObservedAt),
      fetchedText: formatDateTime(item.fetchedAt)
    }
  }
})
