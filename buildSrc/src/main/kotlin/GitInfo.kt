object GitInfo {
    fun gitShortHash(): String {
        return CommandLine.exec("git rev-parse --verify --short HEAD")
    }
}