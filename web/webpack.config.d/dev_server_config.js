const path = require('path');

config.devServer.proxy = {
    '/v1/api': 'http://localhost:8888',
}
config.devServer.historyApiFallback = {
    index: 'index.html',
}

// //root = <project>/build/js/packages/regatta-web
config.devServer.static = {
    directory: path.join(__dirname, '../../../../common/src/commonMain/resources/static'),
}