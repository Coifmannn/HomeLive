// Service worker do HomeLive: deixa o site instalável como app e abre a "casca" (HTML/CSS/JS) mesmo sem rede.
// Os DADOS nunca ficam em cache: tudo que é /api vai sempre direto para o servidor, para ninguém ver informação velha.
const CACHE = 'homelive-casca-v1';

self.addEventListener('install', (e) => {
    e.waitUntil(
        caches.open(CACHE)
            .then((c) => c.addAll(['login.html', 'index.html', 'css/estilo.css', 'js/api.js', 'js/app.js', 'icons/icon-192.png']))
            .then(() => self.skipWaiting())
    );
});

self.addEventListener('activate', (e) => {
    e.waitUntil(
        caches.keys()
            .then((nomes) => Promise.all(nomes.filter((n) => n !== CACHE).map((n) => caches.delete(n))))
            .then(() => self.clients.claim())
    );
});

self.addEventListener('fetch', (e) => {
    const req = e.request;
    const url = new URL(req.url);
    // Só a casca do próprio site; API, outros domínios e métodos que não são GET passam direto.
    if (req.method !== 'GET' || url.origin !== location.origin || url.pathname.startsWith('/api')) return;

    // Rede primeiro (sempre a versão mais nova); se estiver offline, usa a cópia guardada.
    e.respondWith(
        fetch(req)
            .then((res) => {
                if (res.ok) {
                    const copia = res.clone();
                    caches.open(CACHE).then((c) => c.put(req, copia));
                }
                return res;
            })
            .catch(() => caches.match(req))
    );
});
