function composeReady() {
    let app = AppComposables.ComposeApp('root');
    app.notifyReady();
}

window.onload = (event) => {
    let script = document.createElement('script');
    script.type = 'text/javascript';
    script.src = '/regatta.js';

    script.onreadystatechange = composeReady;
    script.onload = composeReady;

    document.head.appendChild(script);
    console.log(`event = ${event}`);
}
