/**
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2007-2013 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
//
// Regenerates the baseapp js/css/images for the current configuration/style.
//

println ''
println 'Prepare:'

// ensure we can execute Sencha CMD
ant.exec(executable: 'sencha', failonerror: true) {
  arg(line: 'which')
}

// generate files for production and testing
[ 'production', 'testing' ].each { flavor ->
  println ''
  println "Generating: $flavor"

  // run 'app build' for each flavor
  ant.mkdir(dir: "target/$flavor")
  ant.exec(executable: 'sencha', dir: 'src/main/baseapp', failonerror: true, output: "target/$flavor/build.log") {
    arg(line: "app build $flavor")
  }
}

println ''
println 'Installing:'

def outdir = new File('src/main/resources/static/rapture').canonicalFile
println "Output directory: $outdir"

// rebuild/clean output structure
ant.mkdir(dir: outdir)
ant.delete(dir: outdir)
ant.mkdir(dir: "$outdir/resources")
ant.mkdir(dir: "$outdir/resources/images")

// install non-debug files
ant.copy(file: 'target/production/BaseApp/app.js', tofile: "$outdir/baseapp.js")
ant.copy(file: 'target/production/BaseApp/resources/BaseApp-all.css', tofile: "$outdir/resources/baseapp.css")

// install debug files
ant.copy(file: 'target/testing/BaseApp/app.js', tofile: "$outdir/baseapp-debug.js")
ant.copy(file: 'target/testing/BaseApp/resources/BaseApp-all.css', tofile: "$outdir/resources/baseapp-debug.css")

// install non-debug images
ant.copy(todir: "$outdir/resources/images", overwrite: true) {
  fileset(dir: "target/production/BaseApp/resources/images") {
    include(name: '**')
  }
}

println ''
println 'Changes:'

ant.exec(executable: 'git') {
  arg(line: 'status -s .')
}

println ''
println 'Done'
println ''

