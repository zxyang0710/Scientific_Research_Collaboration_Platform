const rootId = 'app-message-root'

function root() {
  let element = document.getElementById(rootId)
  if (!element) {
    element = document.createElement('div')
    element.id = rootId
    element.className = 'message-root'
    document.body.appendChild(element)
  }
  return element
}

function show(message, type = 'info', duration = 2600) {
  const item = document.createElement('div')
  item.className = `app-message app-message--${type}`
  item.textContent = message
  root().appendChild(item)
  requestAnimationFrame(() => item.classList.add('is-visible'))
  setTimeout(() => {
    item.classList.remove('is-visible')
    setTimeout(() => item.remove(), 220)
  }, duration)
}

export const ElMessage = {
  info: (message) => show(message, 'info'),
  success: (message) => show(message, 'success'),
  warning: (message) => show(message, 'warning'),
  error: (message) => show(message, 'error')
}
