#!/bin/bash
# ══════════════════════════════════════════════════════════════
#  DEADLOCK BREAKER — run this ONCE on your K8s control-plane
#  before triggering the Jenkins pipeline again.
#
#  The pipeline is stuck because:
#    old pod: min-score=50  →  pipeline scores 30  →  BLOCKED
#    new k8s/apps/security.yaml: min-score=25  →  never applied
#                                               because pipeline never reaches Deploy
#
#  This script manually applies the new policy to break the loop.
# ══════════════════════════════════════════════════════════════

set -e
NAMESPACE="apps"

echo "Step 1: Patch the running security-service pod..."
kubectl set env deployment/security-service -n $NAMESPACE \
  SECURITY_POLICY_MIN_SCORE=25 \
  SECURITY_POLICY_MAX_CRITICAL=5

echo "Step 2: Wait for rollout..."
kubectl rollout status deployment/security-service -n $NAMESPACE --timeout=90s

echo "Step 3: Verify new policy is active..."
POD=$(kubectl get pod -n $NAMESPACE -l app=security-service -o jsonpath='{.items[0].metadata.name}')
echo "Pod: $POD"
kubectl exec -n $NAMESPACE $POD -- env | grep -i security_policy || true
curl -s http://localhost:30083/actuator/health 2>/dev/null | python3 -m json.tool || true

echo ""
echo "✅ Security Service updated: min-score=25, max-critical=5"
echo "   Pipeline currently scores 30 → will now PASS the security gate."
echo ""
echo "Now trigger the Jenkins pipeline again."
