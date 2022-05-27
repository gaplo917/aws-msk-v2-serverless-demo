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

        const loadBalancer = new ApplicationLoadBalancer(this, 'MSKDemoLB', {
            loadBalancerName: 'msk-demo-alb',
            vpc: vpcStack.vpc,
            securityGroup: vpcStack.loadBalancerSecurityGroup,
            internetFacing: true
        });

        const albFargateService = new ecs_patterns.ApplicationLoadBalancedFargateService(this, 'MSKDemoFargateService', {
            serviceName: "msk-demo-publisher-service",
            cluster: cluster,
            cpu: 4096,
            memoryLimitMiB: 8192,
            desiredCount: 1,
            securityGroups: [vpcStack.fargateSecurityGroup],
            publicLoadBalancer: true,
            loadBalancer: loadBalancer,
            taskImageOptions: {
                image: ecs.ContainerImage.fromEcrRepository(ecrStack.publisherRepo, '0.0.11'),
                enableLogging: true,
                logDriver: ecs.LogDrivers.awsLogs({streamPrefix: 'ktor-publisher'}),
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

        albFargateService.targetGroup.configureHealthCheck({
            path: "/ping",
            interval: Duration.seconds(120),
            unhealthyThresholdCount: 5,
        });

        // TODO: don't need ALB for internal aggregation task, just want to make sure the demo works at this moment
        const loadBalancer2 = new ApplicationLoadBalancer(this, 'MSKDemoLBAggregator', {
            loadBalancerName: 'msk-demo-alb-aggregator',
            vpc: vpcStack.vpc,
            securityGroup: vpcStack.loadBalancerSecurityGroup,
            internetFacing: true
        });

        const albAggregateFargateService = new ecs_patterns.ApplicationLoadBalancedFargateService(this, 'MSKDemoAggregatorFargateService', {
            serviceName: "msk-demo-consumer-service",
            cluster: cluster,
            cpu: 4096,
            memoryLimitMiB: 8192,
            desiredCount: 1,
            securityGroups: [vpcStack.fargateSecurityGroup],
            publicLoadBalancer: true,
            loadBalancer: loadBalancer2,
            taskImageOptions: {
                image: ecs.ContainerImage.fromEcrRepository(ecrStack.consumerRepo, '0.0.15'),
                enableLogging: true,
                logDriver: ecs.LogDrivers.awsLogs({streamPrefix: 'ktor-consumer'}),
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

        albAggregateFargateService.targetGroup.configureHealthCheck({
            path: "/ping",
            interval: Duration.seconds(120),
            unhealthyThresholdCount: 5,
        });

        // const fargateTaskDefinition = new ecs.FargateTaskDefinition(this, 'MSKDemoAggregateTask', {
        //     cpu: 2048,
        //     memoryLimitMiB: 4096,
        //     taskRole: role,
        // });
        //
        // fargateTaskDefinition.addContainer("KtorConsumer", {
        //     image: ecs.ContainerImage.fromEcrRepository(ecrStack.consumerRepo, '0.0.14'),
        //     logging: ecs.LogDrivers.awsLogs({streamPrefix: 'ktor-consumer'}),
        //     environment: {
        //         'BOOTSTRAP_ADDRESS': kafkaBootstrapAddress,
        //         'REGION': this.region,
        //     }
        // });
        //
        // new ecs.FargateService(this, 'MSKDemoFargateAggregateService', {
        //     cluster: cluster,
        //     serviceName: "msk-demo-aggregate-service",
        //     securityGroups: [vpcStack.fargateSecurityGroup],
        //     taskDefinition: fargateTaskDefinition,
        //     desiredCount: 1,
        // });
    }
}