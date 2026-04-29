const { request, formatDateTime, fixed } = require('../../utils/request')

Page({
  data: {
    keyword: '',
    summary: null,
    latest: [],
    loading: true,
    syncing: false,
    error: ''
  },

  autoRefreshTimer: null,

  onLoad() {
    this.loadData()
    this.startAutoRefresh()
  },

  onShow() {
    this.startAutoRefresh()
  },

  onHide() {
    this.stopAutoRefresh()
  },

  onUnload() {
    this.stopAutoRefresh()
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

  openHistory() {
    wx.navigateTo({
      url: '/pages/history/history'
    })
  },

  loadData(options = {}) {
    const silent = !!options.silent
    if (!silent) {
      this.setData({ loading: true, error: '' })
    }
    return Promise.all([
      request({ url: '/summary' }),
      request({ url: '/latest', data: { q: this.data.keyword, limit: 200 } })
    ]).then(([summary, latest]) => {
      this.setData({
        summary: this.decorateSummary(summary),
        latest: latest.map((item) => this.decorateObservation(item))
      })
    }).catch((err) => {
      this.setData({ error: err.message || '加载失败' })
    }).finally(() => {
      if (!silent) {
        this.setData({ loading: false })
      }
    })
  },

  startAutoRefresh() {
    if (this.autoRefreshTimer) return
    this.autoRefreshTimer = setInterval(() => {
      if (!this.data.syncing) {
        this.loadData({ silent: true })
      }
    }, 60000)
  },

  stopAutoRefresh() {
    if (!this.autoRefreshTimer) return
    clearInterval(this.autoRefreshTimer)
    this.autoRefreshTimer = null
  },

  syncNow() {
    if (this.data.syncing) return
    this.setData({ syncing: true, error: '' })
    request({ url: '/sync', method: 'POST', timeout: 120000 })
      .then((result) => {
        wx.showToast({
          title: result && result.status === 'RUNNING' ? '正在同步' : '同步完成',
          icon: 'success'
        })
        return this.loadData()
      })
      .catch((err) => {
        this.setData({ error: err.message || '同步失败' })
      })
      .finally(() => {
        this.setData({ syncing: false })
      })
  },

  openDetail(event) {
    const { lp, device } = event.currentTarget.dataset
    wx.navigateTo({
      url: `/pages/detail/detail?lpNumber=${encodeURIComponent(lp)}&deviceId=${encodeURIComponent(device)}`
    })
  },

  decorateSummary(summary) {
    if (!summary) return null
    return {
      ...summary,
      averageTemperatureText: `${fixed(summary.averageTemperatureC, 1)}°C`,
      dailyHighestAverageTemperatureText: `${fixed(summary.dailyHighestAverageTemperatureC, 1)}°C`,
      dailyLowestAverageTemperatureText: `${fixed(summary.dailyLowestAverageTemperatureC, 1)}°C`,
      averageHumidityText: `${fixed(summary.averageHumidityPercent, 0)}%`,
      averageWindSpeedText: fixed(summary.averageWindSpeed, 1),
      latestObservedText: formatDateTime(summary.latestObservedAt),
      lastFetchedText: formatDateTime(summary.lastFetchedAt)
    }
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
