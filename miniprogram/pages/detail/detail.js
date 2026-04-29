const { request, formatDateTime, fixed } = require('../../utils/request')

Page({
  data: {
    lpNumber: '',
    deviceId: '',
    lamppost: null,
    history: [],
    loading: true,
    error: ''
  },

  onLoad(options) {
    this.setData({
      lpNumber: options.lpNumber || '',
      deviceId: options.deviceId || ''
    })
    this.loadData()
  },

  onPullDownRefresh() {
    this.loadData().finally(() => wx.stopPullDownRefresh())
  },

  loadData() {
    const { lpNumber, deviceId } = this.data
    this.setData({ loading: true, error: '' })
    return Promise.all([
      request({ url: `/lampposts/${encodeURIComponent(lpNumber)}` }),
      request({ url: `/lampposts/${encodeURIComponent(lpNumber)}/history`, data: { deviceId, limit: 48 } })
    ]).then(([lamppost, history]) => {
      this.setData({
        lamppost: this.decorateLamppost(lamppost),
        history: history.map((item) => this.decorateObservation(item))
      })
    }).catch((err) => {
      this.setData({ error: err.message || '加载失败' })
    }).finally(() => {
      this.setData({ loading: false })
    })
  },

  openMap() {
    const lamppost = this.data.lamppost
    if (!lamppost) return
    wx.openLocation({
      latitude: Number(lamppost.latitude),
      longitude: Number(lamppost.longitude),
      name: lamppost.lpNumber,
      address: '香港智慧灯柱气象观测点',
      scale: 16
    })
  },

  copyCoordinate() {
    const lamppost = this.data.lamppost
    if (!lamppost) return
    wx.setClipboardData({
      data: `${lamppost.latitude}, ${lamppost.longitude}`
    })
  },

  decorateLamppost(lamppost) {
    if (!lamppost) return null
    return {
      ...lamppost,
      deviceLabel: this.data.deviceId || lamppost.deviceIds,
      coordinateText: `${fixed(lamppost.latitude, 6)}, ${fixed(lamppost.longitude, 6)}`,
      updatedText: formatDateTime(lamppost.updatedAt)
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
