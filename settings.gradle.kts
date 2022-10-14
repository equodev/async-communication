pluginManagement {
  repositories {
    maven(url = "https://dl.equo.dev/bndtools/mvn/")
    mavenCentral()
  }
}

plugins {
  id("biz.aQute.bnd.workspace") version "5.3.0"
}
