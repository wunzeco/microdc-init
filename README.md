Microdc Tools Init
==================

## Requirements: 
1. Docker engine
2. Gradle

## Steps
1. Change directory to docker build context directory
2. Ensure ssh directory contains microdc-ci.pem file with 0700 permission. 
    - This file should contain a valid ssh private key that can be used to clone
infra code git repos.
3. Build docker image
    ```
    docker build -t microdc-jenkins .
    ```
	**Note:** Some jenkins plugins may fail to download at first attempt. So
	you may need to run `docker build` command multiple times.
4. Run jenkins container
    ```
    docker run --rm -p 8080:8080 -p 50000:50000 -v $(pwd)/ssh:/var/jenkins\_home/.ssh --name microdc microdc-jenkins
    ```
5. Create jenkins ssh creds config
    ```
    export JENKINS_HOST='192.168.99.100:8080'
    python create-credentials-config-v2.py ssh-private-key \
            -I ci-user-git-creds-id -D ci-user-git-creds \
            -U ci-user -F /var/jenkins_home/.ssh/microdc-ci.pem
    ```
6. Create jenkins jobs
    ```
    cd job-dsl/
    ./gradlew rest -Dpattern=jobs/infraJobs.groovy -DbaseUrl=http://$JENKINS_HOST/
    ```

## ToDo
- ssh-config
- Configure vault password
