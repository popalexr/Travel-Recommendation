import { createApp, h } from 'vue'
import { createInertiaApp } from '@inertiajs/vue3'
import { InertiaProgress } from '@inertiajs/progress'
import './assets/main.css'

createInertiaApp({
    resolve: async (name) => {
        const pages = import.meta.glob('./Pages/**/*.vue')
        const importPage = pages[`./Pages/${name}.vue`]

        if (!importPage) {
            throw new Error(`Unknown Inertia page: ${name}`)
        }

        const module = await importPage()
        const pageComponent = module.default

        if (module.layout) {
            pageComponent.layout = module.layout
        } else if (!pageComponent.layout) {
            const { default: AppLayout } = await import('./Layouts/AppLayout.vue')
            pageComponent.layout = AppLayout
        }

        return module
    },
    setup({ el, App, props, plugin }) {
        createApp({ render: () => h(App, props) })
            .use(plugin)
            .mount(el)
    },
})

InertiaProgress.init()

