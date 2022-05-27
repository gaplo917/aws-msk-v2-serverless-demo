import * as cdk from "aws-cdk-lib";
import * as ecr from "aws-cdk-lib/aws-ecr";
import {Construct} from "constructs";

export class EcrStack extends cdk.Stack {
    public publisherRepo: ecr.Repository;
    public consumerRepo: ecr.Repository;

    constructor(scope: Construct, id: string, props?: cdk.StackProps) {
        super(scope, id, props);

        this.publisherRepo = new ecr.Repository(this, 'PublisherEcrRepo', {
            repositoryName: "msk-demo-ktor-publisher",
            imageTagMutability: ecr.TagMutability.MUTABLE
        })

        this.consumerRepo =  new ecr.Repository(this, 'ConsumerEcrRepo', {
            repositoryName: "msk-demo-ktor-consumer",
            imageTagMutability: ecr.TagMutability.MUTABLE
        })
    }
}