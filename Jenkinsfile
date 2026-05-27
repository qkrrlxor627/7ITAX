pipeline {
    agent any

    stages {
        stage('BE - Docker Build') {
            steps {
                dir('BE') {
                    sh 'DOCKER_BUILDKIT=1 docker build -t tax-backend .'
                }
            }
        }

        stage('FE - Docker Build') {
            steps {
                dir('FE') {
                    sh 'DOCKER_BUILDKIT=1 docker build -t tax-frontend .'
                }
            }
        }

        stage('Prepare AI Model') {
            steps {
                dir('ai') {
                    sh '''
                        mkdir -p models/tax_classifier
                        if [ -d "/opt/tax7i/models/tax_classifier" ]; then
                            cp -r /opt/tax7i/models/tax_classifier/* models/tax_classifier/
                            echo "Model files copied:"
                            ls -lh models/tax_classifier/
                        else
                            echo "WARNING: /opt/tax7i/models/tax_classifier not found. AI will start without classifier."
                        fi
                    '''
                }
            }
        }

        stage('AI - Docker Build') {
            steps {
                dir('ai') {
                    sh 'DOCKER_BUILDKIT=1 docker build -t tax-ai .'
                }
            }
        }

        stage('Deploy') {
            steps {
                sh '''
                    docker stop tax-backend || true
                    docker rm tax-backend || true
                    docker run -d --name tax-backend \
                        --network ubuntu_default \
                        -p 18080:8080 \
                        -e TZ=Asia/Seoul \
                        --env-file /home/ubuntu/.env.backend \
                        -v /home/ubuntu/firebase-service-account.json:/home/ubuntu/firebase-service-account.json:ro \
                        tax-backend

                    docker stop tax-frontend || true
                    docker rm tax-frontend || true
                    docker run -d --name tax-frontend \
                        --network ubuntu_default \
                        -p 3000:80 \
                        tax-frontend

                    docker stop tax-ai || true
                    docker rm tax-ai || true
                    docker run -d --name tax-ai \
                        --network ubuntu_default \
                        -p 19000:8000 \
                        --env-file /home/ubuntu/.env.ai \
                        -v chroma-data:/app/data/chroma \
                        tax-ai
                '''
            }
        }
    }

    post {
        success {
            mattermostSend(
                color: '#2EB67D',
                message: "✅ 배포 성공: ${env.JOB_NAME} #${env.BUILD_NUMBER}\n${env.BUILD_URL}"
            )
        }
        failure {
            mattermostSend(
                color: '#E01E5A',
                message: "❌ 빌드 실패: ${env.JOB_NAME} #${env.BUILD_NUMBER}\n${env.BUILD_URL}"
            )
        }
    }
}
