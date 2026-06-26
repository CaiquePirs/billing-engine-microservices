#!/bin/bash

echo "Starting LocalStack infrastructure setup..."

REGION=us-east-1
ACCOUNT_ID=000000000000

# ============================================================
# SNS TOPICS
# ============================================================

echo "Creating SNS Topics..."

awslocal sns create-topic --name subscription-created-topic
awslocal sns create-topic --name payment-approved-topic
awslocal sns create-topic --name payment-failed-topic

echo "SNS Topics created."

# ============================================================
# SQS QUEUES
# ============================================================

echo "Creating SQS Queues..."

# Payment
awslocal sqs create-queue --queue-name process-payment-queue

# Notification
awslocal sqs create-queue --queue-name notify-new-subscription-queue
awslocal sqs create-queue --queue-name notify-payment-approved-queue
awslocal sqs create-queue --queue-name notify-payment-failed-queue

# Invoice
awslocal sqs create-queue --queue-name generate-invoice-queue
awslocal sqs create-queue --queue-name invoice-created-queue

# Subscription
awslocal sqs create-queue --queue-name active-subscription-queue
awslocal sqs create-queue --queue-name desactivate-subscription-queue

# DLQ
awslocal sqs create-queue --queue-name payment-processing-dlq

echo "SQS Queues created."

# ============================================================
# SNS → SQS SUBSCRIPTIONS
# ============================================================

echo "Creating SNS subscriptions..."

# subscription-created-topic
#   ├── process-payment-queue
#   └── notify-new-subscription-queue
awslocal sns subscribe \
  --topic-arn arn:aws:sns:$REGION:$ACCOUNT_ID:subscription-created-topic \
  --protocol sqs \
  --notification-endpoint arn:aws:sqs:$REGION:$ACCOUNT_ID:process-payment-queue

awslocal sns subscribe \
  --topic-arn arn:aws:sns:$REGION:$ACCOUNT_ID:subscription-created-topic \
  --protocol sqs \
  --notification-endpoint arn:aws:sqs:$REGION:$ACCOUNT_ID:notify-new-subscription-queue

# payment-approved-topic
#   ├── notify-payment-approved-queue
#   ├── generate-invoice-queue
#   └── active-subscription-queue
awslocal sns subscribe \
  --topic-arn arn:aws:sns:$REGION:$ACCOUNT_ID:payment-approved-topic \
  --protocol sqs \
  --notification-endpoint arn:aws:sqs:$REGION:$ACCOUNT_ID:notify-payment-approved-queue

awslocal sns subscribe \
  --topic-arn arn:aws:sns:$REGION:$ACCOUNT_ID:payment-approved-topic \
  --protocol sqs \
  --notification-endpoint arn:aws:sqs:$REGION:$ACCOUNT_ID:generate-invoice-queue

awslocal sns subscribe \
  --topic-arn arn:aws:sns:$REGION:$ACCOUNT_ID:payment-approved-topic \
  --protocol sqs \
  --notification-endpoint arn:aws:sqs:$REGION:$ACCOUNT_ID:active-subscription-queue

# payment-failed-topic
#   ├── notify-payment-failed-queue
#   └── desactivate-subscription-queue
awslocal sns subscribe \
  --topic-arn arn:aws:sns:$REGION:$ACCOUNT_ID:payment-failed-topic \
  --protocol sqs \
  --notification-endpoint arn:aws:sqs:$REGION:$ACCOUNT_ID:notify-payment-failed-queue

awslocal sns subscribe \
  --topic-arn arn:aws:sns:$REGION:$ACCOUNT_ID:payment-failed-topic \
  --protocol sqs \
  --notification-endpoint arn:aws:sqs:$REGION:$ACCOUNT_ID:desactivate-subscription-queue

echo "SNS Subscriptions created."

# ============================================================
# S3 BUCKETS
# ============================================================

echo "Creating S3 Buckets..."

awslocal s3 mb s3://billing-invoices

echo "S3 Buckets created."
echo "LocalStack infrastructure setup completed."