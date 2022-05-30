import * as cdk from "aws-cdk-lib";
import * as ecs from "aws-cdk-lib/aws-ecs";
import * as ecs_patterns from "aws-cdk-lib/aws-ecs-patterns";
import * as iam from "aws-cdk-lib/aws-iam";
import {VpcStack} from "./vpc-stack";
import {Construct} from 'constructs';
import {ApplicationLoadBalancer, ApplicationProtocol} from "aws-cdk-lib/aws-elasticloadbalancingv2";
import {EcrStack} from "./ecr-stack";
import {kafkaBootstrapAddress} from "./kafka-stack";
import {Duration} from "aws-cdk-lib";

export class FargateStack extends cdk.Stack {

    constructor(vpcStack: VpcStack, ecrStack: EcrStack, scope: Construct, id: string, props?: cdk.StackProps) {
        super(scope, id, props);

        const cluster = new ecs.Cluster(this, 'Cluster', {
            clusterName: "msk-demo-ecs-cluster",
            vpc: vpcStack.vpc
        });

        const role = new iam.Role(this, 'MSKDemoFargateServiceRole', {
            assumedBy: new iam.ServicePrincipal('ecs-tasks.amazonaws.com'),
            description: 'MSKDemo fargate role',
        });

        //TODO: harden security
        role.addToPolicy(new iam.PolicyStatement({
                effect: iam.Effect.ALLOW,
                actions: ["kafka-cluster:*"],
                resources: ["*"]
            }
        ))

        const apiLoadBalancer = new ApplicationLoadBalancer(this, 'MSKDemoApiLB', {
            loadBalancerName: 'msk-demo-api-alb',
            vpc: vpcStack.vpc,
            securityGroup: vpcStack.loadBalancerSecurityGroup,
            internetFacing: true,
            idleTimeout: Duration.seconds(60)
        });

        // external facing
        const albFargateProducerService = new ecs_patterns.ApplicationLoadBalancedFargateService(this, 'MSKDemoProducerFargateService', {
            serviceName: "msk-demo-ktor-producer",
            cluster: cluster,
            cpu: 1024,
            memoryLimitMiB: 2048,
            desiredCount: 1,
            securityGroups: [vpcStack.fargateSecurityGroup],
            publicLoadBalancer: true,
            loadBalancer: apiLoadBalancer,
            taskImageOptions: {
                image: ecs.ContainerImage.fromEcrRepository(ecrStack.producerRepo, '0.2.0'),
                enableLogging: true,
                logDriver: ecs.LogDrivers.awsLogs({streamPrefix: 'ktor-producer'}),
                environment: {
                    'BOOTSTRAP_ADDRESS': kafkaBootstrapAddress,
                    'REGION': this.region,
                },
                containerPort: 8080,
                taskRole: role,
            },
            targetProtocol: ApplicationProtocol.HTTP,
            protocol: ApplicationProtocol.HTTP,
            listenerPort: 80,
            openListener: true,
        });

        albFargateProducerService.targetGroup.configureHealthCheck({
            path: "/ping",
            interval: Duration.seconds(120),
            unhealthyThresholdCount: 5,
        });

        const wsLoadBalancer = new ApplicationLoadBalancer(this, 'MSKDemoWSLB', {
            loadBalancerName: 'msk-demo-ws-alb',
            vpc: vpcStack.vpc,
            securityGroup: vpcStack.loadBalancerSecurityGroup,
            internetFacing: true,
            idleTimeout: Duration.seconds(300)
        });

        const albFargateWebSocketService = new ecs_patterns.ApplicationLoadBalancedFargateService(this, 'MSKDemoWebSocketFargateService', {
            serviceName: "msk-demo-ktor-websocket",
            cluster: cluster,
            cpu: 1024,
            memoryLimitMiB: 2048,
            desiredCount: 1,
            securityGroups: [vpcStack.fargateSecurityGroup],
            publicLoadBalancer: true,
            loadBalancer: wsLoadBalancer,
            taskImageOptions: {
                image: ecs.ContainerImage.fromEcrRepository(ecrStack.webSocketRepo, '0.2.1'),
                enableLogging: true,
                logDriver: ecs.LogDrivers.awsLogs({streamPrefix: 'ktor-websocket'}),
                environment: {
                    'BOOTSTRAP_ADDRESS': kafkaBootstrapAddress,
                    'REGION': this.region,
                },
                containerPort: 8080,
                taskRole: role,
            },
            targetProtocol: ApplicationProtocol.HTTP,
            protocol: ApplicationProtocol.HTTP,
            listenerPort: 80,
            openListener: true,
        });

        albFargateWebSocketService.targetGroup.configureHealthCheck({
            path: "/ping",
            interval: Duration.seconds(120),
            unhealthyThresholdCount: 5,
        });

        // background job without exposing a ALB
        const dataAggregatorTaskDef = new ecs.FargateTaskDefinition(this, 'MSKDemoDataAggregatorTask', {
            cpu: 2048,
            memoryLimitMiB: 4096,
            taskRole: role,
        });

        dataAggregatorTaskDef.addContainer("KtorDataAggregator", {
            image: ecs.ContainerImage.fromEcrRepository(ecrStack.dataAggregatorRepo, '0.2.0'),
            logging: ecs.LogDrivers.awsLogs({streamPrefix: 'ktor-data-aggregator'}),
            environment: {
                'BOOTSTRAP_ADDRESS': kafkaBootstrapAddress,
                'REGION': this.region,
            }
        });

        new ecs.FargateService(this, 'MSKDemoDataAggregatorFargateService', {
            cluster: cluster,
            serviceName: "msk-demo-ktor-data-aggregator-service",
            securityGroups: [vpcStack.fargateSecurityGroup],
            taskDefinition: dataAggregatorTaskDef,
            desiredCount: 1,
        });
    }
}