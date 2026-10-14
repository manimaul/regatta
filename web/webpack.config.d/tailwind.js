// Runs Tailwind (via PostCSS) over app.css only. Other CSS (maplibre-gl) keeps the plain Kotlin-generated rule.
;(function (config) {
    const path = require('path');
    const appCss = /app\.css$/;
    const cssRule = config.module.rules.find(r => String(r.test) === String(/\.css$/));
    cssRule.exclude = appCss;
    config.module.rules.push({
        test: appCss,
        use: [
            {loader: 'style-loader', options: {}},
            {loader: 'css-loader', options: {}},
            {
                loader: 'postcss-loader',
                options: {
                    postcssOptions: {
                        plugins: [
                            // webpack.config.js is generated in build/js/packages/<pkg>/, so this resolves to web/src/jsMain/kotlin
                            require('@tailwindcss/postcss')({base: path.resolve(__dirname, '../../../../web/src/jsMain/kotlin')}),
                        ],
                    },
                },
            },
        ],
    });
})(config);
