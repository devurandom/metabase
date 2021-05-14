import React from "react";
import PropTypes from "prop-types";
import { t } from "ttag";

import Button from "metabase/components/Button";
import Tooltip from "metabase/components/Tooltip";

function QuestionActionButtons({ canWrite, onOpenModal }) {
  return (
    <div className="p1 flex justify-start align-center column-gap-1">
      {canWrite && (
        <Tooltip tooltip={t`Edit this question`}>
          <Button
            onlyIcon
            icon="edit_document"
            iconSize={18}
            onClick={() => onOpenModal("edit")}
          />
        </Tooltip>
      )}
      <Tooltip tooltip={t`Add to a dashboard`}>
        <Button
          onlyIcon
          icon="add_to_dash"
          iconSize={18}
          onClick={() => onOpenModal("add-to-dashboard")}
        />
      </Tooltip>
      <div className="py1 pr1 mr1 border-right" aria-hidden="true" />
      {canWrite && (
        <Tooltip tooltip={t`Duplicate this question`}>
          <Button
            onlyIcon
            icon="clone"
            iconSize={18}
            className="text-medium"
            onClick={() => onOpenModal("clone")}
          />
        </Tooltip>
      )}
      {canWrite && (
        <Tooltip tooltip={t`Move`}>
          <Button
            onlyIcon
            icon="move"
            iconSize={18}
            className="text-medium"
            onClick={() => onOpenModal("move")}
          />
        </Tooltip>
      )}
      {canWrite && (
        <Tooltip tooltip={t`Archive`}>
          <Button
            onlyIcon
            icon="archive"
            iconSize={18}
            className="text-medium"
            onClick={() => onOpenModal("archive")}
          />
        </Tooltip>
      )}
    </div>
  );
}

QuestionActionButtons.propTypes = {
  canWrite: PropTypes.bool,
  onOpenModal: PropTypes.func,
};

export default QuestionActionButtons;
