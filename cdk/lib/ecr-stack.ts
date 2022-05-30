import * as cdk from "aws-cdk-lib";
import * as ecr from "aws-cdk-lib/aws-ecr";
import {Construct} from "constructs";

export class EcrStack extends cdk.Stack {
    public producerRepo: ecr.Repository;
    public dataAggregatorRepo: ecr.Repository;
    public webSocketRepo: ecr.Repository;

    constructor(scope: Construct, id: string, props?: cdk.StackProps) {
        super(scope, id, props);

        this.producerRepo = new ecr.Repository(this, 'ProducerEcrRepo', {
            repositoryName: "msk-demo-ktor-producer",
            imageTagMutability: ecr.TagMutability.MUTABLE
        })

        this.dataAggregatorRepo =  new ecr.Repository(this, 'DataAggregatorEcrRepo', {
            repositoryName: "msk-demo-ktor-data-aggregator",
            imageTagMutability: ecr.TagMutability.MUTABLE
        })

        this.webSocketRepo =  new ecr.Repository(this, 'WebSocketEcrRepo', {
            repositoryName: "msk-demo-ktor-websocket",
            imageTagMutability: ecr.TagMutability.MUTABLE
        })
    }
}