Generates a preview for a mindustry map, along with printing other stuff about the map.

#### Usage
- `java -jar previewer.jar map.msav` to create a preview for `map.msav` outputs to cwd, output filename is same as input filename.
- `java -jar previewer.jar map.msav false` to only print map info and not make a preview.

#### Compiling
Requires java 14
- `./gradlew dist` to compile, built jar should be in `build/libs/previewer.jar`

----
Previewing code ~~stolen~~ borrowed from [CoreBot](https://github.com/Anuken/CoreBot)
