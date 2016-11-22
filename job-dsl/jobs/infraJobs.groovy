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
