import React from "react";
import PropTypes from "prop-types";
import SidebarContent from "metabase/query_builder/components/SidebarContent";
import QuestionActionButtons from "metabase/questions/components/QuestionActionButtons";
import QuestionActivityTimeline from "metabase/questions/components/QuestionActivityTimeline";
import { PLUGIN_MODERATION_COMPONENTS } from "metabase/plugins";
import { SIDEBAR_VIEWS } from "./constants";
import { ClampedDescription } from "metabase/questions/components/ClampedDescription";
const {
  ModerationIssueActionMenu,
  OpenModerationIssuesButton,
} = PLUGIN_MODERATION_COMPONENTS;

function QuestionDetailsSidebarPanel({ setView, question, onOpenModal }) {
  const canWrite = question.canWrite();
  const description = question.description();

  return (
    <SidebarContent className="full-height px1">
      <div>
        <QuestionActionButtons canWrite={canWrite} onOpenModal={onOpenModal} />
        <ClampedDescription
          className="px2 pb2"
          description={description}
          onEdit={() => onOpenModal("edit")}
        />
        <div className="mx1 pb2 flex justify-between border-row-divider">
          <ModerationIssueActionMenu
            triggerClassName="Button--round text-brand border-brand py1"
            onAction={issueType => {
              setView({
                name: SIDEBAR_VIEWS.CREATE_ISSUE_PANEL,
                props: { issueType },
              });
            }}
          />
          <OpenModerationIssuesButton
            onClick={() => {
              setView({
                name: SIDEBAR_VIEWS.OPEN_ISSUES_PANEL,
              });
            }}
          />
        </div>
        <QuestionActivityTimeline className="px2 pt2" question={question} />
      </div>
    </SidebarContent>
  );
}

QuestionDetailsSidebarPanel.propTypes = {
  setView: PropTypes.func.isRequired,
  question: PropTypes.object.isRequired,
  onOpenModal: PropTypes.func.isRequired,
};

export default QuestionDetailsSidebarPanel;
