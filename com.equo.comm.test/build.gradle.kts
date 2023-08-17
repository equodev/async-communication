import aQute.bnd.gradle.TestOSGi

tasks.register<TestOSGi>("testosgi.88") {
    doFirst {
        System.setProperty("chromium.version", "88")
    }
    setBndrun("bnd.bnd")
}

tasks.register<TestOSGi>("testosgi.swing") {
    doFirst {
        System.setProperty("chromium.version", "95")
        System.setProperty("toolkit", "SWING")
    }
    setBndrun("bnd.bnd")
}

/*
tasks.register<TestOSGi>("testosgi.windowless") {
    doFirst {
        System.setProperty("chromium.version", "95")
        System.setProperty("toolkit", "WINDOWLESS")
    } 
    setBndrun("bnd.bnd")
}

tasks.register<TestOSGi>("testosgi.88") {
    doFirst {
        System.setProperty("chromium.version", "88")
    }
    setBndrun("bnd.bnd")
}

tasks.register<TestOSGi>("testosgi.80") {
    doFirst {
        System.setProperty("chromium.version", "80")
    }
    setBndrun("bnd.bnd")
}
*/

tasks.named("testOSGi") {
    doFirst {
        System.setProperty("chromium.version", "95")
    }
}

task("printChromiumVersion") {
    doLast {
        System.setProperty("chromium.version", "80")
        val bnd : aQute.bnd.gradle.BndProperties? = project.findProperty("bnd") as aQute.bnd.gradle.BndProperties?
          println("${bnd?.get("chromium.lower.version")?.toString()}-${bnd?.get("chromium.upper.version")?.toString()}")

          System.setProperty("chromium.version", "88")
          println("${bnd?.get("chromium.lower.version")?.toString()}-${bnd?.get("chromium.upper.version")?.toString()}")

          System.setProperty("chromium.version", "95")
          println("${bnd?.get("chromium.lower.version")?.toString()}-${bnd?.get("chromium.upper.version")?.toString()}")
      }
}
