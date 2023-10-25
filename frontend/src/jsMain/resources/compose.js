function composeReady() {
    let counterController = AppComposables.ComposeApp('root');
    counterController.setCount(0);
}

window.onload = (event) => {
    let script = document.createElement('script');
    script.type = 'text/javascript';
    script.src = 'frontend.js';

    script.onreadystatechange = composeReady;
    script.onload = composeReady;

    document.head.appendChild(script);
    console.log(`event = ${event}`);
}
