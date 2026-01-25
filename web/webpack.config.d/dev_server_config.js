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