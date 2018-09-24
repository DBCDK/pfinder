pipeline {
    agent { label "devel8" }
    tools {
        maven "maven 3.5"
    }
    environment {
        MAVEN_OPTS = "-XX:+TieredCompilation -XX:TieredStopAtLevel=1"
    }
    triggers {
        pollSCM("H/3 * * * *")
    }
    options {
        buildDiscarder(logRotator(artifactDaysToKeepStr: "", artifactNumToKeepStr: "", daysToKeepStr: "30", numToKeepStr: "30"))
        timestamps()
    }
    stages {
        stage("build") {
            steps {
                // Fail Early..
                script {
                    if (! env.BRANCH_NAME) {
                        currentBuild.rawBuild.result = Result.ABORTED
                        throw new hudson.AbortException('Job Started from non MultiBranch Build')
                    } else {
                        println(" Building BRANCH_NAME == ${BRANCH_NAME}")
                    }

                }

                sh """
                    mvn -B clean
                    mvn -B install pmd:pmd javadoc:aggregate
                    rm -rf ~/.m2/repositor/dk/dbc/open-search*

                """
            }
        }

        stage('Docker') {
            steps {
                script {
                    def allDockerFiles = findFiles glob: '**/Dockerfile'
                    def dockerFiles = allDockerFiles.findAll { f -> f.path.endsWith("src/main/docker/Dockerfile") }
                    def version = readMavenPom().version

                    for (def f : dockerFiles) {
                        def dirName = f.path.take(f.path.length() - 11)

                        dir(dirName) {
                            modulePom = readMavenPom file: '../../../pom.xml'
                            def projectArtifactId = modulePom.getArtifactId()
                            if( !projectArtifactId ) {
                                throw new hudson.AbortException("Unable to find module ArtifactId in ${dirName}/../../../pom.xml remember to add a <ArtifactId> element")
                            }

                            def imageName = "${projectArtifactId}-${version}".toLowerCase()
                            if (! env.CHANGE_BRANCH) {
                                imageLabel = env.BRANCH_NAME
                            } else {
                                imageLabel = env.CHANGE_BRANCH
                            }
                            if ( ! (imageLabel ==~ /master|trunk/) ) {
                                println("Using branch_name ${imageLabel}")
                                imageLabel = imageLabel.split(/\//)[-1]
                                imageLabel = imageLabel.toLowerCase()
                            } else {
                                println(" Using Master branch ${BRANCH_NAME}")
                                imageLabel = env.BUILD_NUMBER
                            }

                            println("In ${dirName} build ${projectArtifactId} as ${imageName}:$imageLabel")
                            sh 'rm -f *.war ; cp ../../../target/*.war . || true ; if [ -e prepare.sh ] ; then chmod +x prepare.sh ; ./prepare.sh ; fi'
                            def app = docker.build("$imageName:${imageLabel}".toLowerCase(), '--pull --no-cache .')

                            if (currentBuild.resultIsBetterOrEqualTo('SUCCESS')) {
                                docker.withRegistry('https://docker-os.dbc.dk', 'docker') {
                                    app.push()
                                    if (env.BRANCH_NAME ==~ /master|trunk/) {
                                        app.push "latest"
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        stage("upload") {
            steps {
                script {
                    if (env.BRANCH_NAME ==~ /master|trunk/) {
                        sh """
                            mvn jar:jar deploy:deploy
                        """
                    }
                }
            }
        }
    }
}
