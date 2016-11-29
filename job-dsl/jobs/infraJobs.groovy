/*
    tools infra build/update/delete/config-upload job
*/

def product = 'microdc'
def rakeScriptsRepo = 'ee-microdc-rake-tf-build'
def toolsDslRepo    = 'ee-microdc-tools'

/*
    product infra code pipeline job
*/
[ 'deploy', 'destroy' ].each {
    def action = it

    pipelineJob("${product}-infra-${action}") {
        parameters {
            stringParam('AWS_DEFAULT_REGION', defaultValue = 'eu-west-1', 
                        description = 'AWS Default Region')
            stringParam('AWS_ACCESS_KEY_ID', defaultValue = '', 
                        description = 'AWS Access Key ID')
            stringParam('AWS_SECRET_ACCESS_KEY', defaultValue = '', 
                        description = 'AWS Secret Key')
            choiceParam('DC_ENVIRONMENT', ['tools'], 'Environment to provision')
        }         
        definition {
            cpsScm {
                scm {
                    git {
                        remote {
                            github("EqualExperts/${toolsDslRepo}", 'ssh')
                            credentials("ci-user-git-creds-id")
                        }
                        branch('master')
                        extensions {
                            relativeTargetDirectory("${toolsDslRepo}")
                        }
                    }
                }
                scriptPath("${toolsDslRepo}/jenkinsfiles/init-${action}-Jenkinsfile")
            }
        }
    }
}

pipelineJob("${product}-infra-global") {
    parameters {
        stringParam('AWS_DEFAULT_REGION', defaultValue = 'eu-west-1', 
                    description = 'AWS Default Region')
        stringParam('AWS_ACCESS_KEY_ID', defaultValue = '', 
                    description = 'AWS Access Key ID')
        stringParam('AWS_SECRET_ACCESS_KEY', defaultValue = '', 
                    description = 'AWS Secret Key')
        choiceParam('DC_ENVIRONMENT', ['global'], 'Environment to provision')
    }         
    definition {
        cpsScm {
            scm {
                git {
                    remote {
                        github("EqualExperts/${toolsDslRepo}", 'ssh')
                        credentials("ci-user-git-creds-id")
                    }
                    branch('master')
                    extensions {
                        relativeTargetDirectory("${toolsDslRepo}")
                    }
                }
            }
            scriptPath("${toolsDslRepo}/jenkinsfiles/init-global-Jenkinsfile")
        }
    }
}


/*
    create s3 bucket job
*/
job("microdc-infra-state-s3-bucket") {
    description("Creates s3 bucket for microdc infra state")
    parameters {
        stringParam('AWS_DEFAULT_REGION', defaultValue = 'eu-west-1', 
                    description = 'AWS Default Region')
        stringParam('AWS_ACCESS_KEY_ID', defaultValue = '', 
                    description = 'AWS Access Key ID')
        stringParam('AWS_SECRET_ACCESS_KEY', defaultValue = '', 
                    description = 'AWS Secret Key')
    }         
    scm {
        git {
            remote {
                github("EqualExperts/${rakeScriptsRepo}", 'ssh')
                credentials("ci-user-git-creds-id")
            }
            branch('master')
        }
    }
    steps {
        environmentVariables {
            env('AWS_ACCESS_KEY_ID',     '\$AWS_ACCESS_KEY_ID')
            env('AWS_SECRET_ACCESS_KEY', '\$AWS_SECRET_ACCESS_KEY')
            env('AWS_DEFAULT_REGION',    '\$AWS_DEFAULT_REGION')
            env('DC_PRODUCT',            'microdc')
            env('DC_CATEGORY',           'microdc')
            env('DC_BUCKET_NAME',        'microdc-infra')
            env('DC_RAKE_SCRIPTS_PATH',  "\$WORKSPACE/${rakeScriptsRepo}")
            env('DC_CREATE_IF_NOT_EXIST', 'true')
        }
        shell(
            """
            #!/bin/bash
            source /etc/profile.d/rvm.sh &> /dev/null
            cd \$DC_RAKE_SCRIPTS_PATH
            gem install bundler && bundle install && rake tf:create_bucket
            """.stripIndent().trim()
            )
    }
    wrappers {
        colorizeOutput()
    }
}


/*
    remote jenkins jobs config
*/
job("microdc-jenkins-jobs-config") {
    description("Create jobs on remote jenkins")
    parameters {
        stringParam('USERNAME', defaultValue = 'microdc-ci', description = 'Remote Jenkins Username')
        stringParam('PASSWORD', defaultValue = '', description = 'Remote Jenkins Password')
        stringParam('BASE_URL', defaultValue = 'https://jenkins.tool.microdc.equalexperts.io/', 
                    description = 'Jenkins Base URL')
    }         
    scm {
        git {
            remote {
                github("EqualExperts/ee-microdc-jenkins-job-dsl", 'ssh')
                credentials("ci-user-git-creds-id")
            }
            branch('master')
        }
    }
    steps {
        gradle {
            tasks('rest')
            switches("-Dpattern=jobs/seed.groovy -DbaseUrl=\${BASE_URL} -Dusername=\${USERNAME} -Dpassword='\${PASSWORD}'")
        }
    }
    wrappers {
        colorizeOutput()
    }
}
