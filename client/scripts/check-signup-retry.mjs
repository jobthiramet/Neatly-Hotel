import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const source = readFileSync(new URL('../src/views/SignUpView.vue', import.meta.url), 'utf8')
const body = source.split('async function verify() {')[1].split('\n</script>')[0]
const calls = []
const signUp = { value: {
  attemptEmailAddressVerification: async () => {
    calls.push('verify')
    return { status: 'complete', createdSessionId: 'session-test' }
  },
} }
const completedSessionId = { value: null }
const isSignedIn = { value: false }
const loading = { value: false }
const verify = new Function('signUp', 'verificationKind', 'error', 'loading',
  'completedSessionId', 'isSignedIn', 'finishProfile', 'finishRegistration',
  'verificationCode', 'prepareNextVerification', 'showRegistrationError',
  `return async function verify() {${body}`)(
  signUp, { value: 'email_address' }, { value: '' }, loading,
  completedSessionId, isSignedIn,
  async () => { calls.push('save-profile') },
  async (sessionId) => {
    completedSessionId.value = sessionId
    isSignedIn.value = true
    signUp.value = null
    throw new Error('Profile service unavailable')
  },
  { value: '123456' }, async () => {}, () => { calls.push('error') },
)

await verify()
assert.equal(loading.value, false)
await verify()
assert.deepEqual(calls, ['verify', 'error', 'save-profile'])
assert.equal(loading.value, false)
console.log('Signup retry passed: profile saving retries without verifying the email twice.')
