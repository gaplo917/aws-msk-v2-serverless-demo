import * as cdk from "aws-cdk-lib";
import * as ec2 from "aws-cdk-lib/aws-ec2";
import { Construct } from 'constructs';
import * as iam from "aws-cdk-lib/aws-iam";

export class VpcStack extends cdk.Stack {
    public vpc: ec2.Vpc;
    public kafkaSecurityGroup: ec2.SecurityGroup;
    public fargateSecurityGroup: ec2.SecurityGroup;
    public loadBalancerSecurityGroup: ec2.SecurityGroup
    public bastionSecurityGroup: ec2.SecurityGroup

    constructor(scope: Construct, id: string, props?: cdk.StackProps) {
        super(scope, id, props);

        this.vpc = new ec2.Vpc(this, 'vpc', );

        this.kafkaSecurityGroup = new ec2.SecurityGroup(this, 'kafkaSecurityGroup', {
            securityGroupName: 'kafkaSecurityGroup',
            vpc: this.vpc,
            allowAllOutbound: true
        });

        this.fargateSecurityGroup = new ec2.SecurityGroup(this, 'fargateSecurityGroup', {
            securityGroupName: 'fargateSecurityGroup',
            vpc: this.vpc,
            allowAllOutbound: true
        });

        this.loadBalancerSecurityGroup = new ec2.SecurityGroup(this, 'loadBalancerSecurityGroup', {
            securityGroupName: 'loadBalancerSecurityGroup',
            vpc: this.vpc,
            allowAllOutbound: true
        });

        this.bastionSecurityGroup = new ec2.SecurityGroup(this, 'bastionSecurityGroup', {
            securityGroupName: 'bastionSecurityGroup',
            vpc: this.vpc,
            allowAllOutbound: true
        });

        this.kafkaSecurityGroup.connections.allowFrom(this.fargateSecurityGroup, ec2.Port.allTraffic(), "allowFromFargateToKafka");
        this.kafkaSecurityGroup.connections.allowFrom(this.bastionSecurityGroup, ec2.Port.allTraffic(), "allowFromBastionToKafka");

        this.fargateSecurityGroup.connections.allowFrom(this.kafkaSecurityGroup, ec2.Port.allTraffic(), "allowFromKafkaToFargate");
        this.fargateSecurityGroup.connections.allowFrom(this.bastionSecurityGroup, ec2.Port.allTraffic(), "allowFromBastionToFargate");
        this.fargateSecurityGroup.connections.allowFrom(this.kafkaSecurityGroup, ec2.Port.allTraffic(), "allowFromKafkaToFargate");

        this.loadBalancerSecurityGroup.addIngressRule(
            ec2.Peer.anyIpv4(),
            ec2.Port.tcp(8080),
            'allow 8080 port traffic from anywhere',
        );
        this.loadBalancerSecurityGroup.addIngressRule(
            ec2.Peer.anyIpv4(),
            ec2.Port.tcp(80),
            'allow HTTP traffic from anywhere',
        );
        this.loadBalancerSecurityGroup.addIngressRule(
            ec2.Peer.anyIpv4(),
            ec2.Port.tcp(443),
            'allow HTTPS traffic from anywhere',
        )

    }
}