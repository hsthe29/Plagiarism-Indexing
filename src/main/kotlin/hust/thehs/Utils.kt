package hust.thehs

import java.io.File

fun listDir(directoryPath: String): Pair<List <String>, List <String>> {
    val directory = File(directoryPath)
    assert(directory.isDirectory) { "Require Directory but found File instead!" }

    val subItems = directory.listFiles()
    val files = subItems?.filter { it.isFile }?.map{ it.path } ?: listOf()
    val subDirs = subItems?.filter { it.isDirectory }?.map { it.path } ?: listOf()
    return subDirs to files
}


fun absolutePathOf(path: String): String {
    val classLoader = Thread.currentThread().contextClassLoader
    val absolutePath = classLoader.getResource(path)

    return absolutePath?.path?.drop(1) ?: path
}


fun isInDirectory(directoryPath: String, checkPath: String): Boolean {
    val rootPath = File(directoryPath).canonicalPath
    val check = File(checkPath).canonicalPath
//    val areRelated: Boolean = file.getCanonicalPath().contains(dir.getCanonicalPath() + File.separator)
//    println(areRelated)
    return check.startsWith(rootPath + File.separator)
}