<template>
    <div style="max-width: 520px; margin: 24px auto; padding: 16px; border: 1px solid #eee; border-radius: 12px;">
        <div style="display:flex; justify-content: space-between; align-items:center; gap: 12px;">
            <h2 style="margin: 0;">Auth</h2>
            <div v-if="isLoggedIn" style="display:flex; align-items:center; gap: 8px;">
                <span style="font-size: 12px; color: #666;">Logged In</span>
                <button @click="onLogout" :disabled="loading" style="padding: 6px 10px;">Logout</button>
            </div>
        </div>

        <div style="margin-top: 12px; display:flex; gap: 8px;">
            <button @click="mode = 'login'" :disabled="loading" :style="tabStyle(mode === 'login')">
                Login
            </button>
            <button @click="mode = 'register'" :disabled="loading" :style="tabStyle(mode === 'register')">
                Register
            </button>
        </div>

        <div style="margin-top: 12px;">
            <div v-if="msg" :style="msgStyle">{{ msg }}</div>
        </div>

        <form v-if="mode === 'login'" @submit.prevent="onLogin"
            style="margin-top: 12px; display:flex; flex-direction: column; gap: 10px;">
            <label style="display:flex; flex-direction: column; gap: 6px;">
                <span style="font-size: 12px; color:#555;">Username</span>
                <input v-model.trim="loginForm.username" autocomplete="username"
                    style="padding: 10px; border: 1px solid #ddd; border-radius: 8px;" />
            </label>

            <label style="display:flex; flex-direction: column; gap: 6px;">
                <span style="font-size: 12px; color:#555;">Password</span>
                <input v-model.trim="loginForm.password" type="password" autocomplete="current-password"
                    style="padding: 10px; border: 1px solid #ddd; border-radius: 8px;" />
            </label>

            <button type="submit" :disabled="loading"
                style="padding: 10px 12px; border-radius: 10px; border: 1px solid #ddd;">
                {{ loading ? 'Loading...' : 'Login' }}
            </button>
        </form>

        <form v-else @submit.prevent="onRegister"
            style="margin-top: 12px; display:flex; flex-direction: column; gap: 10px;">
            <label style="display:flex; flex-direction: column; gap: 6px;">
                <span style="font-size: 12px; color:#555;">Username</span>
                <input v-model.trim="registerForm.username" autocomplete="username"
                    style="padding: 10px; border: 1px solid #ddd; border-radius: 8px;" />
            </label>

            <label style="display:flex; flex-direction: column; gap: 6px;">
                <span style="font-size: 12px; color:#555;">Password</span>
                <input v-model.trim="registerForm.password" type="password" autocomplete="new-password"
                    style="padding: 10px; border: 1px solid #ddd; border-radius: 8px;" />
            </label>

            <label style="display:flex; flex-direction: column; gap: 6px;">
                <span style="font-size: 12px; color:#555;">Email (optional)</span>
                <input v-model.trim="registerForm.email" autocomplete="email"
                    style="padding: 10px; border: 1px solid #ddd; border-radius: 8px;" />
            </label>

            <button type="submit" :disabled="loading"
                style="padding: 10px 12px; border-radius: 10px; border: 1px solid #ddd;">
                {{ loading ? 'Loading...' : 'Register' }}
            </button>
        </form>

        <div style="margin-top: 16px; padding-top: 12px; border-top: 1px solid #eee;">
            <div style="display:flex; justify-content: space-between; align-items:center; gap: 12px;">
                <h3 style="margin: 0; font-size: 14px;">Me</h3>
                <button @click="loadMe" :disabled="loading || !isLoggedIn" style="padding: 6px 10px;">Refresh</button>
            </div>
            <pre
                style="margin-top: 10px; padding: 12px; background: #fafafa; border: 1px solid #eee; border-radius: 10px; overflow:auto;">{{ meText }}</pre>
        </div>
    </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import axios from 'axios'

type Mode = 'login' | 'register'

const mode = ref<Mode>('login')
const loading = ref(false)
const msg = ref('')
const msgType = ref<'info' | 'error'>('info')
const meText = ref('')

const loginForm = reactive({
    username: '',
    password: ''
})

const registerForm = reactive({
    username: '',
    password: '',
    email: ''
})

const tokenKey = 'auth_token'

const isLoggedIn = computed(() => {
    return !!localStorage.getItem(tokenKey)
})

function setMsg(text: string, type: 'info' | 'error' = 'info') {
    msg.value = text
    msgType.value = type
}

function tabStyle(active: boolean) {
    return {
        padding: '8px 12px',
        borderRadius: '10px',
        border: '1px solid ' + (active ? '#333' : '#ddd'),
        background: active ? '#111' : '#fff',
        color: active ? '#fff' : '#333'
    } as Record<string, string>
}

const msgStyle = computed(() => {
    const base: Record<string, string> = {
        padding: '10px 12px',
        borderRadius: '10px',
        border: '1px solid #eee',
        fontSize: '13px'
    }
    if (msgType.value === 'error') {
        return { ...base, background: '#fff5f5', borderColor: '#ffd6d6', color: '#a40000' }
    }
    return { ...base, background: '#f5fff7', borderColor: '#d6ffe0', color: '#116329' }
})

async function onLogin() {
    setMsg('')
    loading.value = true
    try {
        const resp = await axios.post('/api/auth/login', {
            username: loginForm.username,
            password: loginForm.password
        })

        const data = resp.data
        const token = typeof data === 'string' ? data : (data?.token as string | undefined)
        localStorage.setItem(tokenKey, token || 'logged-in')
        setMsg('Login success')
        await loadMe()
    } catch (e: any) {
        setMsg(e?.response?.data?.message || e?.message || 'Login failed', 'error')
    } finally {
        loading.value = false
    }
}

async function onRegister() {
    setMsg('')
    loading.value = true
    try {
        await axios.post('/api/auth/register', {
            username: registerForm.username,
            password: registerForm.password,
            email: registerForm.email || undefined
        })
        setMsg('Register success, please login')
        mode.value = 'login'
    } catch (e: any) {
        setMsg(e?.response?.data?.message || e?.message || 'Register failed', 'error')
    } finally {
        loading.value = false
    }
}

async function onLogout() {
    setMsg('')
    loading.value = true
    try {
        await axios.post('/api/auth/logout')
    } catch {
    } finally {
        localStorage.removeItem(tokenKey)
        meText.value = ''
        setMsg('Logged out')
        loading.value = false
    }
}

async function loadMe() {
    setMsg('')
    if (!isLoggedIn.value) {
        meText.value = ''
        return
    }

    loading.value = true
    try {
        const resp = await axios.get('/api/user/me')
        meText.value = typeof resp.data === 'string' ? resp.data : JSON.stringify(resp.data, null, 2)
    } catch (e: any) {
        meText.value = ''
        setMsg(e?.response?.data?.message || e?.message || 'Load me failed', 'error')
    } finally {
        loading.value = false
    }
}

onMounted(() => {
    if (isLoggedIn.value) {
        loadMe()
    }
})
</script>
