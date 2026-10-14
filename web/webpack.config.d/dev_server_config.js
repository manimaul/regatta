// Only jsBrowserDevelopmentRun has a dev server; plain webpack builds and Karma tests don't define config.devServer.
if (config.devServer) {
    const path = require('path');

    config.devServer.proxy = [
        {
            context: ["/v1/api"],
            target: "http://localhost:8888",
        },
    ]

    config.devServer.historyApiFallback = {
        index: 'index.html',
    }

    config.devServer.static = {
        directory: path.join(__dirname, '../../../../common/src/commonMain/resources/static'),
    }
}
