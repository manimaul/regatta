# Regatta
Software for scoring sailing regattas utilizing [PHRF](https://www.ussailing.org/competition/offshore/phrf/) scoring.
Developed by William Kamp of SV Waymaker for the [Corinthian Yacht Club of Tacoma](https://cyct.com/).

![Sailing Vessel Waymaker](./sv_waymaker.jpeg "SV Waymaker")

### Build and run 
`./gradlew run`

#### Run in browser hot loading
`./gradlew jsRun --continuous`

### Building and using the output
`./gradlew jsBrowserProductionWebpack`
This will produce the output in `build/dist/js/productionExecutable`.

### Useful links:
- [Use Kotlin Code from JS](https://kotlinlang.org/docs/js-to-kotlin-interop.html)
- [JavaScript modules](https://kotlinlang.org/docs/js-modules.html)
- [webpack bundling](https://kotlinlang.org/docs/js-project-setup.html#webpack-bundling)
