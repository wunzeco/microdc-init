FROM jenkins:2.19.2

# if we want to install via apt
USER root
RUN apt-get update && \
    apt-get install -y ruby build-essential ansible python-pip jq unzip \
	                   bison zlib1g-dev libssl-dev libxml2-dev 			\
                       gawk libreadline6-dev libyaml-dev libsqlite3-dev \
					   sqlite3 autoconf libgmp-dev libgdbm-dev libncurses5-dev \
					   automake libtool pkg-config libffi-dev

RUN pip install awscli

ENV TERRAFORM_VERSION 0.7.11
RUN wget -O /var/tmp/terraform_${TERRAFORM_VERSION}_linux_amd64.zip \
         https://releases.hashicorp.com/terraform/${TERRAFORM_VERSION}/terraform_${TERRAFORM_VERSION}_linux_amd64.zip && \
		 unzip /var/tmp/terraform_${TERRAFORM_VERSION}_linux_amd64.zip -d /usr/local/bin/


# Install RVM
RUN gpg --keyserver hkp://keys.gnupg.net --recv-keys 409B6B1796C275462A1703113804BB82D39DC0E3 && \
    curl -sSL https://get.rvm.io | bash -s stable && \
    bash -l -c "rvm install 2.2.3"    && \
    bash -l -c "rvm use 2.2.3@global" && \
    bash -l -c "gem install bundler"
	
#bash -l -c "source /etc/profile.d/rvm.sh"

# Allow jenkins user use rvm
RUN usermod -aG rvm jenkins

# drop back to the regular jenkins user - good practice
USER jenkins

# Disable jenkins setup wizard
ENV  JAVA_OPTS "-Djenkins.install.runSetupWizard=false"

# Install jenkins plugins
RUN /usr/local/bin/install-plugins.sh \
            ace-editor ansicolor bouncycastle-api branch-api cloudbees-folder \
            credentials display-url-api durable-task git git-client \
            git-server github github-api handlebars jquery-detached \
            junit mailer matrix-project momentjs pipeline-build-step \
            pipeline-graph-analysis pipeline-input-step \
            pipeline-milestone-step pipeline-rest-api \
            pipeline-stage-step pipeline-stage-view plain-credentials rebuild \
            scm-api script-security ssh-agent ssh-credentials structs \
            token-macro workflow-aggregator workflow-api \
            workflow-basic-steps workflow-cps workflow-cps-global-lib \
            workflow-durable-task-step workflow-job workflow-multibranch \
            workflow-scm-step workflow-step-api workflow-support

# Start ssh-agent and Add private keys to ssh-agent
# For some reason this config cannot be found on a running container. hmm...Why?
# Perhaps config .profile via a job or use ssh-agent plugin
#RUN echo 'eval $(ssh-agent -s)  &&  ssh-add $HOME/.ssh/*.pem' >> /var/jenkins_home/.profile
