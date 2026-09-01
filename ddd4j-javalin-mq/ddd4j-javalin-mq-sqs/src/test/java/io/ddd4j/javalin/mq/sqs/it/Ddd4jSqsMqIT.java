package io.ddd4j.javalin.mq.sqs.it;

import com.google.inject.Module;
import io.ddd4j.javalin.mq.sqs.Ddd4jSqsMqGuiceModule;
import io.ddd4j.javalin.testcontainers.JunitJupiterTestContainers;
import io.ddd4j.javalin.testcontainers.messaging.AbstractMqIntegrationTest;
import io.ddd4j.mq.listener.MQListener;
import io.ddd4j.mq.sqs.SqsMQClient;
import io.ddd4j.mq.sqs.SqsProperties;
import org.junit.jupiter.api.Tag;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for {@link Ddd4jSqsMqGuiceModule} against LocalStack (AWS SQS emulator,
 * {@code localstack/localstack:3.4}) brought up by Testcontainers, using the AWS SDK v2
 * {@code software.amazon.awssdk:sqs} client. 公共骨架继承自 {@link AbstractMqIntegrationTest}。
 *
 * <p>SQS 差异：SQS 没有 topic 概念——{@link #adaptListenerTopic} 中先建队列，
 * 再把 listener 的 topic 改写为 queue URL；message id 以 SQS message attribute 传递。
 */
@Tag("integration")
@JunitJupiterTestContainers
class Ddd4jSqsMqIT extends AbstractMqIntegrationTest<SqsProperties, SqsMQClient> {

    @SuppressWarnings("resource")
    private static final GenericContainer<?> LOCALSTACK = new GenericContainer<>(
            DockerImageName.parse("localstack/localstack:3.4"))
            .withEnv("SERVICES", "sqs")
            .withExposedPorts(4566)
            .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(2)));

    @Override
    protected String brokerName() {
        return "sqs";
    }

    @Override
    protected String topicName() {
        // 逻辑队列名；物理 topic（queue URL）由 adaptListenerTopic 改写
        return "ddd4j-it-queue";
    }

    @Override
    protected GenericContainer<?> container() {
        return LOCALSTACK;
    }

    @Override
    protected SqsProperties newProperties() {
        SqsProperties props = new SqsProperties();
        props.setRegion("us-east-1");
        props.setAccessKey("test");
        props.setSecretKey("test");
        props.setEndpointOverride("http://" + LOCALSTACK.getHost() + ":" + LOCALSTACK.getMappedPort(4566));
        props.setWaitTimeSeconds(1);
        props.setPollIntervalMs(200);
        return props;
    }

    @Override
    protected SqsMQClient newClient(SqsProperties props) {
        return new SqsMQClient(props);
    }

    @Override
    protected Module guiceModule(SqsMQClient client, SqsProperties props) {
        return new Ddd4jSqsMqGuiceModule(client, props);
    }

    @Override
    protected Class<SqsMQClient> clientClass() {
        return SqsMQClient.class;
    }

    @Override
    protected void adaptListenerTopic(MQListener listener, SqsProperties props) {
        // SQS has no topic: create the queue up front and point the listener at its URL.
        String queueUrl;
        try (SqsClient sqs = props.client()) {
            queueUrl = sqs.createQueue(CreateQueueRequest.builder()
                    .queueName(topicName()).build()).queueUrl();
        }
        assertThat(queueUrl).startsWith("http://");
        listener.setTopic(queueUrl);
    }

    @Override
    protected Duration consumerSettleDelay() {
        // SQS consumer 为轮询线程，init 返回即已就绪，无需额外等待
        return Duration.ZERO;
    }
}
