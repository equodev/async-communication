plugins {
  checkstyle
}

allprojects {
  apply(plugin = "checkstyle")

  checkstyle {
    toolVersion = "8.43"
  }
}
