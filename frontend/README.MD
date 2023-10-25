
### Run in browser
`./gradlew :frontend:jsRun`

#### Run in browser hot loading
`./gradlew :frontend:jsBrowserDevelopmentRun --continuous`

### Building and using the output
`./gradlew :frontend:jsBrowserProductionWebpack`
This will produce the output in `build/dist/js/productionExecutable`.

### Useful links:
- [Use Kotlin Code from JS](https://kotlinlang.org/docs/js-to-kotlin-interop.html)
- [JavaScript modules](https://kotlinlang.org/docs/js-modules.html)
- [webpack bundling](https://kotlinlang.org/docs/js-project-setup.html#webpack-bundling)
