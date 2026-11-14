CREATE TABLE moment_polls (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    moment_id UUID NOT NULL REFERENCES moments(id) ON DELETE CASCADE,
    question VARCHAR(100) NOT NULL,
    option_a VARCHAR(40) NOT NULL,
    option_b VARCHAR(40) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'CLOSED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_moment_polls_moment UNIQUE (moment_id),
    CONSTRAINT ck_moment_polls_question_nonempty CHECK (length(trim(question)) > 0),
    CONSTRAINT ck_moment_polls_option_a_nonempty CHECK (length(trim(option_a)) > 0),
    CONSTRAINT ck_moment_polls_option_b_nonempty CHECK (length(trim(option_b)) > 0)
);
CREATE INDEX ix_moment_polls_moment ON moment_polls(moment_id);

CREATE TABLE moment_poll_votes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    poll_id UUID NOT NULL REFERENCES moment_polls(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    selected_option VARCHAR(1) NOT NULL CHECK (selected_option IN ('A', 'B')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_moment_poll_user UNIQUE (poll_id, user_id)
);
CREATE INDEX ix_moment_poll_votes_poll ON moment_poll_votes(poll_id);

