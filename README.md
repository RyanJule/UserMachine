UserMachine is an API for user registration, authentication, and authorization using PostgreSQL and Spring Boot. The app includes JWT-based security, role-based access, and email verification with an SMTP configuration.

This guide provides setup instructions for deploying the application locally and in a Kubernetes cluster, configuring environment variables, and managing SMTP credentials.
Prerequisites

    Docker - Ensure you have Docker installed to build and run containers locally.
    Kubernetes - Install Minikube or set up access to an existing Kubernetes cluster.
    kubectl - Kubernetes command-line tool to manage cluster resources.
    Java 17 - Required for building and running the Spring Boot application.
    PostgreSQL - Used for the production environment.

Initial Setup

    Clone the Repository

bash

    git clone https://github.com/yourusername/UserMachine.git
    cd UserMachine

Define essential environment variables for the application to connect to PostgreSQL, authenticate with an SMTP server, and handle JWT tokens.

Environment Variables

Add the following variables to your ~/.bashrc (or ~/.zshrc) file to make them persistent across sessions:

bash

  # Database Configuration (Configured host and port here are for development/test environments)
  export DB_HOST=localhost
  export DB_PORT=5432
  export DB_NAME=your_database_name
  export DB_USERNAME=your_database_username
  export DB_PASSWORD=your_database_password
  
  # SMTP Configuration for Email
  export SMTP_EMAIL=your_smtp_username
  export SMTP_PASSWORD=your_smtp_password
  
  # JWT Secret Key
  export JWT_SECRET=your_jwt_secret_key
  
After adding these lines, reload the shell configuration:

bash

  source ~/.bashrc

Kubernetes Secrets Configuration

Store sensitive data, such as database and SMTP credentials, in Kubernetes secrets.

    Create Kubernetes Secrets

bash
  
  kubectl create secret generic db-secret \
    --from-literal=POSTGRES_DB=$DB_NAME \
    --from-literal=POSTGRES_PASSWORD=$DB_USERNAME \
    --from-literal=POSTGRES_USER=$DB_PASSWORD
  
  kubectl create secret generic smtp-secret \
    --from-literal=SMTP_HOST=$SMTP_HOST \
    --from-literal=SMTP_PORT=$SMTP_PORT \
    --from-literal=SMTP_EMAIl=$SMTP_EMAIL \
    --from-literal=SMTP_PASSWORD=$SMTP_PASSWORD
    
  kubectl create secret generic jwt-secret \
    --from-literal=JWT_SECRET_KEY=$JWT_SECRET_KEY
  
Reference Secrets in Deployment Files

In your deployment.yaml file, reference the secrets:

yaml

    env:
      - name: DB_HOST
        valueFrom:
          secretKeyRef:
            name: db-secret
            key: DB_HOST
      - name: SMTP_HOST
        valueFrom:
          secretKeyRef:
            name: smtp-secret
            key: SMTP_HOST
      - name: JWT_SECRET
        valueFrom:
          secretKeyRef:
            name: jwt-secret
            key: JWT_SECRET

Deploying to Kubernetes

    Start Minikube (or ensure your Kubernetes cluster is running)

bash

  minikube start

Apply Persistent Volume Claim

bash

  kubectl apply -f pvc.yaml

Deploy PostgreSQL and UserMachine Services

    PostgreSQL Deployment

bash

  kubectl apply -f postgresql-deployment.yaml

Running Locally with Maven:

For local development, you can use maven to run the application with port forwarding from the PostgreSQL container.

In one terminal:

bash

  kubectl port-forward service/postgres-service 5432:5432

In a separate terminal with cwd ~/UserMachine:

bash

  mvn spring-boot:run

UserMachine Deployment

bash

    kubectl apply -f deployment.yaml

Expose Services

If using Minikube, expose the service to make it accessible from outside the cluster:

bash

  minikube service usermachine-service

Alternatively, for production deployments, use a Kubernetes LoadBalancer or Ingress.

Verify Deployment

Confirm that both containers are running:

bash

  kubectl get pods

Testing Application Availability

Use an HTTP client (such as curl or Postman) to send a request to verify if the API is accessible:

bash

    curl http://<external-ip>:<port>/api/endpoint

SMTP Configuration

To use the email verification and password reset features, set up an SMTP account.

    Choose an SMTP Provider (e.g., Gmail, SendGrid, Amazon SES).
    Add SMTP Credentials as environment variables (as outlined above).
    Configure SMTP Port based on your provider (typically 587 for TLS or 465 for SSL).

Useful Commands

    Check logs for a specific pod

bash

  kubectl logs <pod-name>

Delete all Kubernetes resources

bash

    kubectl delete all --all

Troubleshooting

    Cannot Connect to Database:
        Verify database credentials in both your environment variables and Kubernetes secrets.
        Check if the PostgreSQL pod is running: kubectl get pods.

    SMTP Email Not Sent:
        Verify SMTP credentials.
        Check your SMTP provider’s limitations and restrictions.

    Application Crash or Fails to Start:
        Check application logs using kubectl logs <pod-name> for more details.
        Ensure all environment variables are correctly set.

Contributing

Contributions are welcome! Please submit pull requests with clear descriptions of your changes.
License

This project is licensed under the MIT License.
