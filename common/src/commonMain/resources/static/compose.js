function composeReady() {
    let app = web.ComposeApp('root');
    app.notifyReady();
}

window.onload = (event) => {
    let script = document.createElement('script');
    script.type = 'text/javascript';
    script.src = '/web.js';

    script.onreadystatechange = composeReady;
    script.onload = composeReady;

    document.head.appendChild(script);
    console.log(`event = ${event}`);
}
