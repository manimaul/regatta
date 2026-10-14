// Production builds also emit web.js.br and web.js.gz next to web.js. They get copied into the server's
// static/ resources, and staticResources(...) { preCompressed(...) } in Routing.kt serves whichever the
// browser accepts - Ktor's Compression plugin can only gzip on the fly, never Brotli.
//
// Dev server and Karma builds skip this: they run in development mode and nobody serves the output.
if (config.mode === 'production') {
    const CompressionPlugin = require('compression-webpack-plugin');
    const zlib = require('zlib');
    config.plugins.push(
        new CompressionPlugin({
            test: /\.js$/,
            filename: '[path][base].br',
            algorithm: 'brotliCompress',
            compressionOptions: {params: {[zlib.constants.BROTLI_PARAM_QUALITY]: zlib.constants.BROTLI_MAX_QUALITY}},
        }),
        new CompressionPlugin({
            test: /\.js$/,
            filename: '[path][base].gz',
            algorithm: 'gzip',
            compressionOptions: {level: 9},
        }),
    );
}
