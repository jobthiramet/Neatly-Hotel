// Run with: node scripts/check-agent-access.mjs
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import vm from 'node:vm'

const source = readFileSync(new URL('../src/router/index.ts', import.meta.url), 'utf8')
const guardSource = source.slice(source.indexOf('router.beforeEach('), source.indexOf('// Clerk restores'))
let guard
let session
let allowed = false
let roleError = false
vm.runInNewContext(guardSource, {
  router: { beforeEach: callback => { guard = callback } },
  loadedClerk: async () => session,
  hasAgentRole: async token => {
    assert.equal(token, 'session-token')
    if (roleError) throw new Error('Profile unavailable')
    return allowed
  },
})
const check = async (meta, expected) => {
  const result = await guard({ meta, fullPath: '/admin/hotel-information' })
  assert.equal(result?.name, expected)
}
await check({ requiresAgent: true }, 'agent-login')
await check({ agentLogin: true }, undefined)
session = { user: { id: 'user_test' }, session: { getToken: async () => 'session-token' } }
await check({ requiresAgent: true }, 'agent-forbidden')
await check({ agentLogin: true }, 'agent-forbidden')
allowed = true
await check({ requiresAgent: true }, undefined)
await check({ agentLogin: true }, 'admin-hotel-information')
roleError = true
await check({ requiresAgent: true }, 'agent-forbidden')
roleError = false
session.session.getToken = async () => null
await check({ requiresAgent: true }, 'agent-forbidden')
await check({}, undefined)
console.log('Agent access checks passed (9 cases).')

const layout = readFileSync(new URL('../src/components/layout/AdminLayout.vue', import.meta.url), 'utf8')
const logoutSource = layout.slice(layout.indexOf('async function logout()'), layout.indexOf('</script>'))
const loggingOut = { value: false }
let finishSignOut
let calls = 0
let errorShown = false
const clerk = { value: { signOut: ({ redirectUrl }) => {
  assert.equal(redirectUrl, '/admin/login')
  calls++
  return new Promise(resolve => { finishSignOut = resolve })
} } }
const logout = vm.runInNewContext(`${logoutSource}\nlogout`, {
  clerk, loggingOut, toast: { error: () => { errorShown = true } },
})
const pending = logout()
await logout()
assert.equal(calls, 1)
assert.equal(loggingOut.value, true)
finishSignOut()
await pending
assert.equal(loggingOut.value, false)
clerk.value.signOut = async () => { throw new Error('Network failure') }
await logout()
assert.equal(errorShown, true)
assert.equal(loggingOut.value, false)
console.log('Agent logout checks passed (redirect, duplicate clicks, error recovery).')
