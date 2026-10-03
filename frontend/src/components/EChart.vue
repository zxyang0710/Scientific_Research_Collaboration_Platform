<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts/core'
import { BarChart, PieChart } from 'echarts/charts'
import {
  GridComponent,
  LegendComponent,
  TooltipComponent
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

const props = defineProps({
  option: { type: Object, required: true },
  height: { type: String, default: '260px' }
})

const container = ref(null)
let chart
let observer

echarts.use([
  BarChart,
  PieChart,
  GridComponent,
  LegendComponent,
  TooltipComponent,
  CanvasRenderer
])

function render() {
  if (!container.value) return
  if (!chart) chart = echarts.init(container.value)
  chart.setOption(props.option, true)
}

onMounted(() => {
  render()
  observer = new ResizeObserver(() => chart?.resize())
  observer.observe(container.value)
})

watch(() => props.option, render, { deep: true })

onBeforeUnmount(() => {
  observer?.disconnect()
  chart?.dispose()
})
</script>

<template>
  <div ref="container" :style="{ height }" />
</template>
